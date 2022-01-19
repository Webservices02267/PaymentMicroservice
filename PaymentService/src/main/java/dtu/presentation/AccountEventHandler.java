package dtu.presentation;

import dtu.application.interfaces.IAccountService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Token;
import messaging.Event;
import messaging.EventResponse;
import messaging.MessageQueue;
import messaging.implementations.MockMessageQueue;

import java.util.concurrent.CompletableFuture;

import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.HANDLE.CUSTOMER_VERIFICATION_REQUESTED;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.HANDLE.MERCHANT_VERIFICATION_REQUESTED;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.PUBLISH.CUSTOMER_VERIFICATION_RESPONSE;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.PUBLISH.MERCHANT_VERIFICATION_RESPONSE;

public class AccountEventHandler  {
    public IAccountService accountService;
    public MockTokenService tokenService;
    public MessageQueue messageQueue;
    public CompletableFuture<Event> customerVerified;

    public static int full_payment_timeout_periode = 5000;


    public AccountEventHandler(MockMessageQueue mq) {
        this.messageQueue = mq;
    }


    public Event merchantVerificationRequest(String customerId, String sessionId) {
        customerVerified = new CompletableFuture<Event>();
        EventResponse eventResponse = new EventResponse(sessionId, true, null, customerId);
        Event outgoingEvent = new Event(MERCHANT_VERIFICATION_REQUESTED+"." + sessionId, eventResponse);

        messageQueue.publish(outgoingEvent);
        messageQueue.addHandler(MERCHANT_VERIFICATION_RESPONSE+"." + sessionId, this::handleMerchantVerificationResponse);

        (new Thread() {
            public void run() {
                try {
                    Thread.sleep(full_payment_timeout_periode);
                    EventResponse eventResponse = new EventResponse(sessionId, false, "No response from AccountService");
                    Event value = new Event(MERCHANT_VERIFICATION_RESPONSE+"." + sessionId, eventResponse);
                    customerVerified.complete(value);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return customerVerified.join();
    }

    public void handleMerchantVerificationResponse(Event e) {
        System.err.println(MERCHANT_VERIFICATION_RESPONSE + e);
        customerVerified.complete(e);
    }


    public void handleCustomerVerificationResponse(Event e) {
        System.err.println(CUSTOMER_VERIFICATION_RESPONSE + e);
        customerVerified.complete(e);
    }


    public Event customerVerificationRequest(Token token, String sessionId) {
        customerVerified = new CompletableFuture<Event>();
        EventResponse eventResponse = new EventResponse(sessionId, true, null, token.getUuid());
        Event outgoingEvent = new Event(CUSTOMER_VERIFICATION_REQUESTED+"." + sessionId, eventResponse);

        messageQueue.addHandler(CUSTOMER_VERIFICATION_RESPONSE+"." + sessionId, this::handleCustomerVerificationResponse);
        messageQueue.publish(outgoingEvent);

        (new Thread() {
            public void run() {
                try {
                    Thread.sleep(full_payment_timeout_periode);
                    EventResponse eventResponse = new EventResponse(sessionId, false, "No response from AccountService");
                    Event value = new Event(CUSTOMER_VERIFICATION_RESPONSE+"." + sessionId, eventResponse);
                    customerVerified.complete(value);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return customerVerified.join();
    }
}


