package dtu.presentation;

import dtu.application.IPaymentService;
import dtu.application.interfaces.IAccountService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Payment;
import dtu.domain.Token;
import dtu.exceptions.*;
import messaging.Event;
import messaging.EventResponse;
import messaging.GLOBAL_STRINGS;
import messaging.MessageQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertNull;

public class PaymentEventHandler {



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
        this.messageQueue.addHandler(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.PAYMENT_STATUS_REQUEST, this::handlePaymentStatusRequest);
        this.messageQueue.addHandler(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.PAYMENT_REQUEST, this::handlePaymentRequest);
    }

    public final Map<String, Session> sessions = new ConcurrentHashMap<>();




    /*
    HANDLERS
     */
    public void handlePaymentStatusRequest(Event event) {
        var eventResponse = event.getArgument(0, EventResponse.class);
        messageQueue.publish(new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_STATUS_RESPONSE+ "." + eventResponse.getSessionId(), new EventResponse(eventResponse.getSessionId(), true, null, "OK")));
    }
    // This is done by Payment service (This service)
    /**
     * Send tokenVerificationRequest to TokenService (token)
     * @param e contains MerchantId, TokenId, Amount, Description, SessionId
     */
    public void handlePaymentRequest(Event e) {
        System.err.println("RECEIVED EVENT " + e);
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
        messageQueue.publish(event);
    }

    public void handleMerchantIdToAccountNumberResponse(Event e) {
        var er = e.getArgument(0, EventResponse.class);
        var sid = er.getSessionId();
        var session = sessions.get(sid);
        Event event;
        if (!er.isSuccess()) {
            event = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE + "." + sid, new EventResponse(sid, false, er.getErrorMessage()));
            session.publishedEvents.put(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE, e);
        } else {
            session.merchantAccountNumber = er.getArgument(0, String.class);
            event = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST, new EventResponse(sid, true, null, session.token));
            session.publishedEvents.put(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST, event);
            this.messageQueue.addHandler(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE + "." + sid, this::handleGetCustomerIdFromTokenResponse);
        }
        messageQueue.publish(event);
    }

    public void handleGetCustomerIdFromTokenResponse(Event e) {
        var er = e.getArgument(0, EventResponse.class);
        var sid = er.getSessionId();
        var session = sessions.get(sid);
        if (!er.isSuccess()) {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE + "." + sid, new EventResponse(sid, false, er.getErrorMessage()));
            session.publishedEvents.put(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE, e);
        }
        session.token = er.getArgument(0, Token.class);
        session.customerId = session.token.getCustomerId();
        session.tokenId = session.token.getUuid();
        Event event = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.CUSTOMER_ID_TO_ACCOUNT_NUMBER_REQUEST, new EventResponse(sid, true, null, session.customerId));
        session.publishedEvents.put(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.CUSTOMER_ID_TO_ACCOUNT_NUMBER_REQUEST, event);
        this.messageQueue.addHandler(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, this::handleCustomerIdToAccountNumberResponse);
        messageQueue.publish(event);
    }

    public void handleCustomerIdToAccountNumberResponse(Event e) {
        var er = e.getArgument(0, EventResponse.class);
        var sid = er.getSessionId();
        var session = sessions.get(sid);
        if (!er.isSuccess()) {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE + "." + sid, new EventResponse(sid, false, er.getErrorMessage()));
            session.publishedEvents.put(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE, e);
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
            event = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE + "." + sid, new EventResponse(sid, true, null, GLOBAL_STRINGS.OK_STRINGS.ALL_GOOD));
        } catch (NegativeAmountException | ArgumentNullException | AmountIsNotANumberException | InvalidTokenException
                | DebtorHasNoBankAccountException | CreditorHasNoBankAccountException | InsufficientBalanceException ex) {
            event = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE + "." + sid, new EventResponse(sid, false, ex.getMessage()));
        }
        session.publishedEvents.put(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE, event);
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
    //TODO MOVE PUBLISH PAYMENT RESPONSE TO HANDLERS
    public Event doPaymentRequestEvent(PaymentDTO paymentDTO, String sid) {
        var e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.PAYMENT_REQUEST + "." + sid, new EventResponse(sid, true, null, paymentDTO));
        messageQueue.publish(e);
        return e;
    }

    public Event doMerchantIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasMerchant(session.merchantId)) {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new EventResponse(sid, true, null, accountNumber));
        } else {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new EventResponse(sid, false, "No merchant exists with the provided id"));
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doGetCustomerIdFromTokenResponse(Token token, String sid) {
        Event e;
        var session = sessions.get(sid);
        session.token = tokenService.getVerifiedToken(session.tokenId);
        if (session.token.getValidToken()) {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE + "." + sid, new EventResponse(sid, true, null, session.token));
        } else {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE + "." + sid, new EventResponse(sid, false, "Invalid token"));
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doCustomerIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasCustomer(session.customerId)) {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new EventResponse(sid, true, null, accountNumber));
        } else {
            e = new Event(GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new EventResponse(sid, false, "No customer exists with the provided id"));
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

