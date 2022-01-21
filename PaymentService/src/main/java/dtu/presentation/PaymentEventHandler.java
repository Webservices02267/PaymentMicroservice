package dtu.presentation;

import dtu.application.IPaymentService;
import dtu.application.interfaces.IAccountService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Payment;
import dtu.domain.Token;
import dtu.exceptions.*;
import messaging.Event;
import messaging.EventResponse;
import messaging.MessageQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static messaging.GLOBAL_STRINGS.OK_STRINGS.ALL_GOOD;
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.*;
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.OK_STRINGS.SANITY_CHECK;
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.*;


public class PaymentEventHandler {


    public static int full_payment_timeout_periode = 5000;
    private CompletableFuture<Event> paymentDone;

    public static class Session {
        public String merchantId;
        public String merchantAccountNumber;
        public String customerId;
        public String customerAccountNumber;
        public String tokenId;
        public String amount;
        public String description;
        public Token token;
        public HashMap<String, Event> publishedEvents = new HashMap<>();
    }

    private final MessageQueue messageQueue;

    private final IPaymentService paymentService;
    public IAccountService accountService;
    public MockTokenService tokenService;

    public PaymentEventHandler(MessageQueue messageQueue, IPaymentService paymentService) {
        this.messageQueue = messageQueue;
        this.paymentService = paymentService;
        this.messageQueue.addHandler(PAYMENT_STATUS_REQUESTED, this::handlePaymentStatusRequest);
        this.messageQueue.addHandler(PAYMENT_REQUESTED, this::handlePaymentRequest);
    }

    public final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /*
    HANDLERS
     */
    public void handlePaymentStatusRequest(Event event) {
        var eventResponse = event.getArgument(0, EventResponse.class);
        messageQueue.publish(new Event(PAYMENT_STATUS_RESPONDED + eventResponse.getSessionId(), new EventResponse(eventResponse.getSessionId(), true, null, SANITY_CHECK)));
    }
    // This is done by Payment service (This service)
    /**
     * Send tokenVerificationRequest to TokenService (token)
     * @param e contains MerchantId, TokenId, Amount, Description, SessionId
     */
    public void handlePaymentRequest(Event e) {
        System.err.println("RECEIVED EVENT " + e);
        var eventArgument = e.getArgument(0, EventResponse.class);
        var sessionId = eventArgument.getSessionId();
        var session = new Session();
        session.tokenId = eventArgument.getArgument(0, String.class);
        session.merchantId = eventArgument.getArgument(1, String.class);
        session.amount = eventArgument.getArgument(2, String.class);
        session.description = eventArgument.getArgument(3, String.class);
        sessions.put(eventArgument.getSessionId(), session);
        Event event = new Event(MERCHANT_ID_TO_ACCOUNT_NUMBER_REQUESTED, new EventResponse(sessionId, true, null, session.merchantId));
        session.publishedEvents.put(MERCHANT_ID_TO_ACCOUNT_NUMBER_REQUESTED, event);
        this.messageQueue.addHandler(MERCHANT_TO_ACCOUNT_NUMBER_RESPONDED + eventArgument.getSessionId(), this::handleMerchantIdToAccountNumberResponse);
        System.err.println("PUBLISHED EVENT " + event);
        messageQueue.publish(event);
    }

    public void handleMerchantIdToAccountNumberResponse(Event e) {
        var eventArgument = e.getArgument(0, EventResponse.class);
        var sessionId = eventArgument.getSessionId();
        var session = sessions.get(sessionId);
        Event event;
        if (!eventArgument.isSuccess()) {
            event = new Event(PAYMENT_RESPONDED + sessionId, new EventResponse(sessionId, false, eventArgument.getErrorMessage()));
            session.publishedEvents.put(PAYMENT_RESPONDED, e);
        } else {
            session.merchantAccountNumber = eventArgument.getArgument(0, String.class);
            event = new Event(GET_CUSTOMER_ID_FROM_TOKEN_REQUESTED, new EventResponse(sessionId, true, null, session.tokenId));
            session.publishedEvents.put(GET_CUSTOMER_ID_FROM_TOKEN_REQUESTED, event);
            this.messageQueue.addHandler(GET_CUSTOMER_ID_FROM_TOKEN_REQUESTED + sessionId, this::handleGetCustomerIdFromTokenResponse);
        }
        messageQueue.publish(event);
    }

    public void handleGetCustomerIdFromTokenResponse(Event e) {
        var er = e.getArgument(0, EventResponse.class);
        var sid = er.getSessionId();
        var session = sessions.get(sid);
        if (!er.isSuccess()) {
            e = new Event(PAYMENT_RESPONDED + sid, new EventResponse(sid, false, er.getErrorMessage()));
            session.publishedEvents.put(PAYMENT_RESPONDED, e);
        }
        session.customerId = er.getArgument(0, String.class);
        session.token = new Token(session.customerId, session.tokenId, true);
        Event event = new Event(CUSTOMER_ID_TO_ACCOUNT_NUMBER_REQUESTED, new EventResponse(sid, true, null, session.customerId));
        session.publishedEvents.put(CUSTOMER_ID_TO_ACCOUNT_NUMBER_REQUESTED, event);
        this.messageQueue.addHandler(CUSTOMER_ID_TO_ACCOUNT_NUMBER_REQUESTED + sid, this::handleCustomerIdToAccountNumberResponse);
        messageQueue.publish(event);
    }

    public void handleCustomerIdToAccountNumberResponse(Event e) {
        var er = e.getArgument(0, EventResponse.class);
        var sid = er.getSessionId();
        var session = sessions.get(sid);
        if (!er.isSuccess()) {
            e = new Event(PAYMENT_RESPONDED + sid, new EventResponse(sid, false, er.getErrorMessage()));
            session.publishedEvents.put(PAYMENT_RESPONDED, e);
        }
        session.customerAccountNumber = er.getArgument(0, String.class);
        Event event;
        try {
            var payment = new Payment.PaymentBuilder()
                    .debtor(session.customerAccountNumber)
                    .creditor(session.merchantAccountNumber)
                    .description(session.description)
                    .amount(session.amount)
                    .token(session.token)
                    .build();
            paymentService.pay(payment);
            var logEvent = new Event("LogPaymentRequest", new EventResponse(sid, true, null, new PaymentLogDTO(payment)));
            messageQueue.publish(logEvent);
            event = new Event(PAYMENT_RESPONDED + sid, new EventResponse(sid, true, null, ALL_GOOD));
        } catch (NegativeAmountException | ArgumentNullException | AmountIsNotANumberException | InvalidTokenException
                | DebtorHasNoBankAccountException | CreditorHasNoBankAccountException | InsufficientBalanceException ex) {
            event = new Event(PAYMENT_RESPONDED + sid, new EventResponse(sid, false, ex.getMessage()));
        }
        session.publishedEvents.put(PAYMENT_RESPONDED, event);
        messageQueue.publish(event);
    }

    public static class PaymentLogDTO {

        public PaymentLogDTO() {

        }

        public PaymentLogDTO(Payment payment) {
            this.customerId = payment.getDebtor();
            this.merchantId = payment.getCreditor();
            this.token = payment.getToken().getUuid();
            this.amount = payment.getAmount().toString();
        }

        public String customerId;
        public String merchantId;
        public String token;
        public String amount;
    }



    /*
    MOCK
     */
    public Event doPaymentRequestEvent2(PaymentDTO paymentDTO, String sid) {
        paymentDone = new CompletableFuture<Event>();
        EventResponse eventResponse = new EventResponse(sid, true, null, paymentDTO);
        var outgoingEvent = new Event(PAYMENT_REQUESTED + sid, eventResponse );

        messageQueue.addHandler(PAYMENT_RESPONDED + sid, this::handlePaymentRequest2);
        messageQueue.publish(outgoingEvent);

        new Thread(() -> {
                try {
                Thread.sleep(full_payment_timeout_periode);
                EventResponse eventResponseThread = new EventResponse(sid, false, "No response from PaymentService");
                Event event = new Event(PAYMENT_RESPONDED + sid, eventResponseThread);
                paymentDone.complete(event);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }).start();
        return paymentDone.join();
    }

    public void handlePaymentRequest2(Event e) {
        System.err.println("PaymentResponse" + e);
        paymentDone.complete(e);
        /*System.err.println("RECEIVED EVENT " + e);
        var er = e.getArgument(0, EventResponse.class);
        var sid = er.getSessionId();
        var payment = er.getArgument(0, PaymentDTO.class);
        var session = new Session();
        session.merchantId = payment.merchantId;
        session.tokenId = payment.token;
        session.amount = payment.amount;
        session.description = payment.description;
        sessions.put(er.getSessionId(), session);
        Event event = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.MERCHANT_ID_TO_ACCOUNT_NUMBER_REQUEST, new EventResponse(sid, true, null, session.merchantId));
        session.publishedEvents.put(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.MERCHANT_ID_TO_ACCOUNT_NUMBER_REQUEST, event);
        this.messageQueue.addHandler(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE + "." + er.getSessionId(), this::handleMerchantIdToAccountNumberResponse);
        System.err.println("PUBLISHED EVENT " + event);
        messageQueue.publish(event);*/

    }

    //TODO MOVE PUBLISH PAYMENT RESPONSE TO HANDLERS
    public Event doPaymentRequestEvent(PaymentDTO paymentDTO, String sid) {
        var session = new Session();
        session.tokenId = paymentDTO.token;
        session.merchantId = paymentDTO.merchant;
        session.amount = paymentDTO.amount;
        session.description = paymentDTO.description;
        var e = new Event(PAYMENT_REQUESTED + sid, new EventResponse(sid, true, null, paymentDTO.merchant, paymentDTO.token, paymentDTO.amount, paymentDTO.description));
        messageQueue.publish(e);
        return e;
    }

    public Event doMerchantIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasMerchant(session.merchantId)) {
            e = new Event(MERCHANT_TO_ACCOUNT_NUMBER_RESPONDED + sid, new EventResponse(sid, true, null, accountNumber));
        } else {
            e = new Event(MERCHANT_TO_ACCOUNT_NUMBER_RESPONDED + sid, new EventResponse(sid, false, "No merchant exists with the provided id"));
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doGetCustomerIdFromTokenResponse(Token token, String sid) {
        Event e;
        var session = sessions.get(sid);
        session.token = tokenService.getVerifiedToken(session.tokenId);
        if (session.token.getValidToken()) {
            e = new Event(GET_CUSTOMER_ID_FROM_TOKEN_RESPONDED  + sid, new EventResponse(sid, true, null, session.token.getCustomerId()));
        } else {
            e = new Event(GET_CUSTOMER_ID_FROM_TOKEN_RESPONDED + sid, new EventResponse(sid, false, "Invalid token"));
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doCustomerIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasCustomer(session.customerId)) {
            e = new Event(CUSTOMER_TO_ACCOUNT_NUMBER_RESPONDED  + sid, new EventResponse(sid, true, null, accountNumber));
        } else {
            e = new Event(CUSTOMER_TO_ACCOUNT_NUMBER_RESPONDED+ sid, new EventResponse(sid, false, "No customer exists with the provided id"));
        }
        messageQueue.publish(e);
        return e;
    }




    //This is done by the Rest Service




    //This is done by the TokenService
    /*
     public boolean doTokenVerificationResponseEvent(Token token) {
        Event event = new Event(TOKEN_VERIFICATION_RESPONSE, new Object[]{ token });
        messageQueue.publish(event);
        return event.getArgument(0, Token.class).getValidToken();
    }
     */









}

