package dtu.presentation;

import dtu.application.IPaymentService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Token;
import dtu.exceptions.*;
import messaging.Event;
import messaging.MessageQueue;

import static org.junit.Assert.assertNull;

public class PaymentServiceEventWrapper2 {

    private final MessageQueue messageQueue;
    private final IPaymentService paymentService;
    private final MockTokenService tokenService;


    public PaymentServiceEventWrapper2(MessageQueue messageQueue, IPaymentService paymentService, MockTokenService tokenService) {
        this.messageQueue = messageQueue;
        this.paymentService = paymentService;
        this.tokenService = tokenService;
        this.messageQueue.addHandler(RabbitmqStrings.PAYMENT_REQUEST, this::handlePaymentRequest);
        this.messageQueue.addHandler(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, this::handleTokenVerificationResponse);
    }

    //This is done by the Rest Service
    public void doPaymentRequestEvent(Event e) {
        messageQueue.publish(e);
    }

    //This is done by the TokenService
    public boolean doTokenVerificationResponseEvent(String token) {
        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, new Object[]{ tokenService.getVerifiedToken(token)});
        messageQueue.publish(event);
        return event.getArgument(0, Token.class).getValidToken();
    }

    PaymentDTO payment;
    // This is done by Payment service (This service)
    public void handlePaymentRequest(Event e) {
        payment = e.getArgument(0, PaymentDTO.class);

        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_REQUEST, new Object[]{payment.token});
        messageQueue.publish(event);
    }


    // This is done by Payment service (This service)
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

