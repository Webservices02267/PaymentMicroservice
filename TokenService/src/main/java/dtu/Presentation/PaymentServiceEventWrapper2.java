package dtu.Presentation;

import dtu.Application.BankServiceWrapper;
import dtu.Application.IPaymentService;
import dtu.Application.ITokenService;
import dtu.Application.MockBankService;
import dtu.Domain.AmountIsNotANumberException;
import dtu.Domain.ArgumentNullException;
import dtu.Domain.NegativeAmountException;
import dtu.Domain.Payment;
import dtu.ws.fastmoney.BankService;
import messaging.Event;
import messaging.MessageQueue;

import java.math.BigDecimal;

import static org.junit.Assert.assertNull;

public class PaymentServiceEventWrapper2 {

    private final MessageQueue messageQueue;
    private final IPaymentService paymentService;
    private final ITokenService tokenService;
    private final BankServiceWrapper bankService;


    public PaymentServiceEventWrapper2(MessageQueue messageQueue, IPaymentService paymentService, ITokenService tokenService, BankServiceWrapper bankService) {
        this.messageQueue = messageQueue;
        this.paymentService = paymentService;
        this.tokenService = tokenService;
        this.bankService = bankService;
        this.messageQueue.addHandler(RabbitmqStrings.PAYMENT_REQUEST, this::handlePaymentRequest);
        this.messageQueue.addHandler(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, this::handleTokenVerificationResponse);
    }

    //This is done by the Rest Service
    public void doPaymentRequestEvent(Event e) {
        messageQueue.publish(e);
    }

    //This is done by the TokenService
    public boolean doTokenVerificationResponseEvent(String token, String customerId) {
        // tokenEvent = new Event(event_name, new Object[] { payment.getToken(), true });
        final boolean valid_token = tokenService.verifyToken(customerId, token);
        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, new Object[]{customerId, token, valid_token});
        messageQueue.publish(event);
        return valid_token;
    }

    Payment payment;
    // This is done by Payment service (This service)
    public void handlePaymentRequest(Event e) {

        payment = e.getArgument(0, Payment.class);
        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_REQUEST, new Object[]{payment.getToken()});
        messageQueue.publish(event);
    }


    // This is done by Payment service (This service)
    public Event handleTokenVerificationResponse(Event e) {
        String cid = e.getArgument(0, String.class);
        String token = e.getArgument(1, String.class);
        Boolean valid_token = e.getArgument(2, Boolean.class);

        //Sanity check, that the payment does not include the cid
        assertNull(payment.getCustomerId());

        boolean return_value = false;
        Event event;

        if (valid_token) {
            try {
                //TODO: move this out of presentation layer
                return_value = paymentService.pay(Payment.build(cid, payment.getMerchantId(), payment.getAmount()));
                event = new Event(RabbitmqStrings.PAYMENT_RESPONSE, new Object[]{true, "everything ok"});
            } catch (Exception | NegativeAmountException | ArgumentNullException | AmountIsNotANumberException ex) {
                return_value = false;
                //ex.printStackTrace();
                event = new Event(RabbitmqStrings.PAYMENT_RESPONSE, new Object[]{false, "Insufficient balance"});
            }

        } else {
            event = new Event(RabbitmqStrings.PAYMENT_RESPONSE, new Object[]{false, "invalid token"});
        }

        messageQueue.publish(event);

        return event;
    }
}

