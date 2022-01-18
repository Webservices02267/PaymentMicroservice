package dtu.presentation;

import dtu.application.IPaymentService;
import dtu.application.interfaces.IAccountService;
import dtu.application.mocks.MockAccountService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Payment;
import dtu.domain.Token;
import dtu.exceptions.*;
import messaging.Event;
import messaging.MessageQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertNull;

public class PaymentEventHandler {

    public static class HANDLE {
        public static final String PAYMENT_STATUS_REQUEST = "PaymentStatusRequest";
        public static final String PAYMENT_REQUEST = "PaymentRequest";
        public static final String MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE = "MerchantIdToAccountNumberResponse";
        public static final String GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE = "GetCustomerIdFromTokenResponse";
        public static final String CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE = "CustomerIdToAccountNumberResponse";
    }

    public static class PUBLISH {
        public static final String PAYMENT_STATUS_RESPONSE = "PaymentStatusResponse";
        public static final String MERCHANT_TO_ACCOUNT_NUMBER_REQUEST = "MerchantIdToAccountNumberRequest";
        public static final String GET_CUSTOMER_ID_FROM_TOKEN_REQUEST = "GetCustomerIdFromTokenRequest";
        public static final String CUSTOMER_TO_ACCOUNT_NUMBER_REQUEST = "CustomerIdToAccountNumberRequest";
        public static final String PAYMENT_RESPONSE = "PaymentResponse";
    }

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
        this.messageQueue.addHandler(HANDLE.PAYMENT_STATUS_REQUEST, this::handlePaymentStatusRequest);
        this.messageQueue.addHandler(HANDLE.PAYMENT_REQUEST, this::handlePaymentRequest);
    }

    public final Map<String, Session> sessions = new ConcurrentHashMap<>();




    /*
    HANDLERS
     */
    public void handlePaymentStatusRequest(Event event) {
        messageQueue.publish(new Event(PUBLISH.PAYMENT_STATUS_RESPONSE, new Object[] {"All good"}));
    }
    // This is done by Payment service (This service)
    /**
     * Send tokenVerificationRequest to TokenService (token)
     * @param incomingEvent contains MerchantId, TokenId, Amount, Description, SessionId
     */
    public void handlePaymentRequest(Event incomingEvent) {
        var payment = incomingEvent.getArgument(0, PaymentDTO.class);
        var session = new Session();
        session.merchantId = payment.mid;
        session.tokenId = payment.token;
        session.amount = payment.amount;
        session.description = payment.description;
        sessions.put(payment.sessionId, session);
        Event event = new Event(PUBLISH.MERCHANT_TO_ACCOUNT_NUMBER_REQUEST, new Object[]{session.merchantId, payment.sessionId});
        session.publishedEvents.put(PUBLISH.MERCHANT_TO_ACCOUNT_NUMBER_REQUEST, event);
        this.messageQueue.addHandler(HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE + "." + payment.sessionId, this::handleMerchantIdToAccountNumberResponse);
        messageQueue.publish(event);
    }

    public void handleMerchantIdToAccountNumberResponse(Event e) {
        boolean success = e.getArgument(0, boolean.class);
        var sid = e.getType().split("\\.")[1];
        var session = sessions.get(sid);
        if (!success) {
            e = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[] {false, "Creditor account is not valid"});
            session.publishedEvents.put(PUBLISH.PAYMENT_RESPONSE, e);
        }
        session.merchantAccountNumber = e.getArgument(1, String.class);
        Event event = new Event(PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST, new Object[]{session.token, sid});
        session.publishedEvents.put(PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST, event);
        this.messageQueue.addHandler(HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE + "." + sid, this::handleGetCustomerIdFromTokenResponse);
        messageQueue.publish(event);
    }

    public void handleGetCustomerIdFromTokenResponse(Event e) {
        boolean success = e.getArgument(0, boolean.class);
        var sid = e.getType().split("\\.")[1];
        var session = sessions.get(sid);
        if (!success) {
            e = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[] {false, "Token must be valid"});
            session.publishedEvents.put(PUBLISH.PAYMENT_RESPONSE, e);
        }
        session.token = e.getArgument(1, Token.class);
        session.customerId = session.token.getCustomerId();
        session.tokenId = session.token.getUuid();
        Event event = new Event(PUBLISH.CUSTOMER_TO_ACCOUNT_NUMBER_REQUEST, new Object[]{session.customerId, sid});
        session.publishedEvents.put(PUBLISH.CUSTOMER_TO_ACCOUNT_NUMBER_REQUEST, event);
        this.messageQueue.addHandler(HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, this::handleCustomerIdToAccountNumberResponse);
        messageQueue.publish(event);
    }

    public void handleCustomerIdToAccountNumberResponse(Event e) {
        var sid = e.getType().split("\\.")[1];
        var session = sessions.get(sid);
        boolean success = e.getArgument(0, boolean.class);
        if (!success) {
            e = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[] {false, "Token must be valid"});
            session.publishedEvents.put(PUBLISH.PAYMENT_RESPONSE, e);
        }
        session.customerAccountNumber = e.getArgument(1, String.class);
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
            var logEvent = new Event("LogPaymentRequest", new Object[]{new PaymentLogDTO(payment)});
            messageQueue.publish(logEvent);
            event = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[]{ true, "ok"});
        } catch (NegativeAmountException | ArgumentNullException | AmountIsNotANumberException | InvalidTokenException
                | DebtorHasNoBankAccountException | CreditorHasNoBankAccountException | InsufficientBalanceException ex) {
            event = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[]{ false, ex.getMessage() });
        }
        session.publishedEvents.put(PUBLISH.PAYMENT_RESPONSE, event);
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
        var e = new Event(HANDLE.PAYMENT_REQUEST + "." + sid, new Object[] {paymentDTO});
        messageQueue.publish(e);
        return e;
    }

    public Event doMerchantIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasMerchant(session.merchantId)) {
            e = new Event(HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new Object[] {true, accountNumber});
        } else {
            e = new Event(HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new Object[] {false, "No merchant exists with the provided id"});
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doGetCustomerIdFromTokenResponse(Token token, String sid) {
        Event e;
        var session = sessions.get(sid);
        session.token = tokenService.getVerifiedToken(session.tokenId);
        if (session.token.getValidToken()) {
            e = new Event(HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE + "." + sid, new Object[] {true, session.token});
        } else {
            e = new Event(HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE + "." + sid, new Object[] {false, "Invalid token"});
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doCustomerIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasCustomer(session.customerId)) {
            e = new Event(HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new Object[] {true, accountNumber});
        } else {
            e = new Event(HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new Object[] {false, "No customer exists with the provided id"});
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

