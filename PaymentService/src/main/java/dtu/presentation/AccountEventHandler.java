package dtu.presentation;

import dtu.application.interfaces.IAccountService;
import dtu.application.mocks.MockTokenService;
import dtu.presentation.PaymentEventHandler;
import messaging.Event;
import messaging.EventResponse;
import messaging.GLOBAL_STRINGS;
import messaging.MessageQueue;
import messaging.implementations.MockMessageQueue;

import java.util.concurrent.CompletableFuture;

import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.HANDLE.MERCHANT_VERIFICATION_REQUESTED;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.PUBLISH.MERCHANT_VERIFICATION_RESPONSE;
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.PAYMENT_REQUEST;

public class AccountEventHandler  {
    public IAccountService accountService;
    public MockTokenService tokenService;
    public MessageQueue messageQueue;
    public CompletableFuture<Event> customerVerified;

    public static int full_payment_timeout_periode = 5000;


    public AccountEventHandler(MockMessageQueue mq, IAccountService accountService) {
        this.messageQueue = mq;
    }


    public Event customerVerificationRequest(String customerId, String sessionId) {
        customerVerified = new CompletableFuture<Event>();
        EventResponse eventResponse = new EventResponse(sessionId, true, null, customerId);
        Event outgoingEvent = new Event(MERCHANT_VERIFICATION_REQUESTED, eventResponse);

        messageQueue.addHandler(MERCHANT_VERIFICATION_RESPONSE+"." + sessionId, this::handleCustomerVerificationResponse);
        messageQueue.publish(outgoingEvent);

        (new Thread() {
            public void run() {
                try {
                    Thread.sleep(5000);
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

    // Handler for verification response from CustomerService in the form of a boolean to see if the customer is registered.
    public void handleCustomerVerificationResponse(Event e) {
        System.err.println(MERCHANT_VERIFICATION_RESPONSE + e);
        customerVerified.complete(e);
    }

}


