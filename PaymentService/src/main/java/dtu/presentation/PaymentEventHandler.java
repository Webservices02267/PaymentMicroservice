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

    public class Session {
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
        this.messageQueue.addHandler(HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE, this::handleMerchantIdToAccountNumberResponse);
        this.messageQueue.addHandler(HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE, this::handleGetCustomerIdFromTokenResponse);
        this.messageQueue.addHandler(HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE, this::handleCustomerIdToAccountNumberResponse);
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
        messageQueue.publish(event);
    }

    public void handleMerchantIdToAccountNumberResponse(Event e) {

        var sid = e.getType().split("\\.")[1];
        var session = sessions.get(sid);
        session.merchantAccountNumber = e.getArgument(0, String.class);
        Event event = new Event(PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST, new Object[]{session.token, sid});
        session.publishedEvents.put(PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST, event);
        messageQueue.publish(event);
    }

    public void handleGetCustomerIdFromTokenResponse(Event e) {
        var sid = e.getType().split("\\.")[1];
        var session = sessions.get(sid);
        session.token = e.getArgument(0, Token.class);
        session.customerId = session.token.getCustomerId();
        session.tokenId = session.token.getUuid();
        Event event = new Event(PUBLISH.CUSTOMER_TO_ACCOUNT_NUMBER_REQUEST, new Object[]{session.customerId, sid});
        session.publishedEvents.put(PUBLISH.CUSTOMER_TO_ACCOUNT_NUMBER_REQUEST, event);
        messageQueue.publish(event);
    }

    public void handleCustomerIdToAccountNumberResponse(Event e) {
        var sid = e.getType().split("\\.")[1];
        var session = sessions.get(sid);
        session.customerAccountNumber = e.getArgument(0, String.class);
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
            event = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[]{ true, "ok"});
        } catch (NegativeAmountException | ArgumentNullException | AmountIsNotANumberException | InvalidTokenException
                | DebtorHasNoBankAccountException | CreditorHasNoBankAccountException | InsufficientBalanceException ex) {
            event = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[]{ false, ex.getMessage() });
        }
        session.publishedEvents.put(PUBLISH.PAYMENT_RESPONSE, event);
        messageQueue.publish(event);
    }





    /*
    MOCK
     */
    public Event doPaymentRequestEvent(PaymentDTO paymentDTO, String sid) {
        var e = new Event(HANDLE.PAYMENT_REQUEST + "." + sid, new Object[] {paymentDTO});
        messageQueue.publish(e);
        return e;
    }

    public Event doMerchantIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasMerchant(session.merchantId)) {
            e = new Event(HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new Object[] {accountNumber});
        } else {
            e = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[] {"Creditor account is not valid"});
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doGetCustomerIdFromTokenResponse(Token token, String sid) {
        Event e;
        var session = sessions.get(sid);
        session.token = tokenService.getVerifiedToken(session.tokenId);
        if (session.token.getValidToken()) {
            e = new Event(HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE + "." + sid, new Object[] {session.token});
        } else {
            e = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[] {"Token must be valid"});
            session.publishedEvents.put(PUBLISH.PAYMENT_RESPONSE, e);
        }
        messageQueue.publish(e);
        return e;
    }

    public Event doCustomerIdToAccountNumberResponse(String accountNumber, String sid) {
        Event e;
        var session = sessions.get(sid);
        if (accountService.hasCustomer(session.customerId)) {
            e = new Event(HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE + "." + sid, new Object[] {accountNumber});
        } else {
            e = new Event(PUBLISH.PAYMENT_RESPONSE + "." + sid, new Object[] {"Debtor account is not valid"});
            session.publishedEvents.put(PUBLISH.PAYMENT_RESPONSE, e);
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

