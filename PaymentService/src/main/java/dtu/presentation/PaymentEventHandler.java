package dtu.presentation;

import dtu.application.IPaymentService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Token;
import dtu.exceptions.*;
import messaging.Event;
import messaging.MessageQueue;

import static org.junit.Assert.assertNull;

public class PaymentEventHandler {

    private final MessageQueue messageQueue;
    private final IPaymentService paymentService;
    PaymentDTO payment;

    public PaymentEventHandler(MessageQueue messageQueue, IPaymentService paymentService) {
        this.messageQueue = messageQueue;
        this.paymentService = paymentService;
        this.messageQueue.addHandler(RabbitmqStrings.PAYMENT_REQUEST, this::handlePaymentRequest);
        this.messageQueue.addHandler(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, this::handleTokenVerificationResponse);
    }

    //This is done by the Rest Service
    public void doPaymentRequestEvent(Event e) {
        messageQueue.publish(e);
    }

    //This is done by the TokenService
    public boolean doTokenVerificationResponseEvent(Token token) {
        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, new Object[]{ token });
        messageQueue.publish(event);
        return event.getArgument(0, Token.class).getValidToken();
    }


    // This is done by Payment service (This service)
    /**
     * Send tokenVerificationRequest to TokenService (token)
     * @param e contains mid,token and amount
     */
    public void handlePaymentRequest(Event e) {
        payment = e.getArgument(0, PaymentDTO.class);

        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_REQUEST, new Object[]{payment.token});
        messageQueue.publish(event);
    }


    // This is done by Payment service (This service)
    /**
     * Send PaymentResponse to RestService (valid?,status)
     * if valid token
     *      do transaction with SOAP Bank and
     *      log the payment in the paymentService with mid,token,amount,cid and status
     * @param e contains cid,token,valid?
     */
    public Event handleTokenVerificationResponse(Event e) {

        Event event;
        Token token = e.getArgument(0, Token.class);
        try {
            paymentService.pay(payment, token);
            event = new Event(RabbitmqStrings.PAYMENT_RESPONSE, new Object[]{ true, "everything ok"});
        } catch (NegativeAmountException | ArgumentNullException | AmountIsNotANumberException | InvalidTokenException | DebtorHasNoBankAccountException | CreditorHasNoBankAccountException | InsufficientBalanceException ex) {
            event = new Event(RabbitmqStrings.PAYMENT_RESPONSE, new Object[]{ false, ex.getMessage() });
        }
        messageQueue.publish(event);
        return event;
    }
}

