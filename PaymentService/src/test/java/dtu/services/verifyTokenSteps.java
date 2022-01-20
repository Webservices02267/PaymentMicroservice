package dtu.services;

import dtu.application.BankServiceWrapper;
import dtu.application.IPaymentService;
import dtu.application.PaymentServiceImplementation;
import dtu.application.interfaces.IAccountService;
import dtu.application.mocks.MockAccountService;
import dtu.application.mocks.MockBankService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Token;
import dtu.infrastructure.InMemoryRepository;
import dtu.presentation.PaymentDTO;
import dtu.presentation.PaymentEventHandler;
import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceException_Exception;
import dtu.ws.fastmoney.User;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import messaging.Event;
import messaging.EventResponse;
import messaging.implementations.MockMessageQueue;
import org.junit.Before;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static dtu.presentation.PaymentEventHandler.full_payment_timeout_periode;
import static io.restassured.RestAssured.sessionId;
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE;
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST;
import static org.junit.Assert.*;

public class verifyTokenSteps {

    BankService bankService = new MockBankService();
    BankServiceWrapper bankServiceWrapper = new BankServiceWrapper(bankService);

    IAccountService accountService = new MockAccountService();
    MockTokenService tokenService = new MockTokenService();
    MockMessageQueue mq = new MockMessageQueue();
    TokenEventHandler eventHandler = new TokenEventHandler(mq, tokenService);



    String customerId;
    String merchantId;
    Token token;
    String sid;
    String balance;
    String amount;

    EventResponse tokenEventResponse;

    @Before
    public void beforeStatement() {
        full_payment_timeout_periode = 5000;
    }

    @After
    public void afterStatement() {
        try {
            bankServiceWrapper.retireAccount(customerId);
            bankServiceWrapper.retireAccount(merchantId);
        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
    }


    @Given("A customer with a token")
    public void aCustokentomerWithAToken() {
        String session_id = "RandomSessionID";

        balance = "1000";
        amount = "100";
        var merchant = new User();
        merchant.setCprNumber("135413-0505");
        merchant.setLastName("merchant");
        merchant.setFirstName("merchant");
        var user = new User();
        user.setCprNumber("135643-1337");
        user.setLastName("customer");
        user.setFirstName("customer");

        try {
            customerId = bankServiceWrapper.createAccountWithBalance(user, new BigDecimal(balance));
            merchantId = bankServiceWrapper.createAccountWithBalance(merchant, new BigDecimal(balance));

        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
        accountService.registerCustomer(customerId);
        token = tokenService.createTokens(customerId, 3).stream().findFirst().get();
        accountService.registerMerchant(merchantId);

        sid = session_id;
    }

    @Given("A customer with an invalid token")
    public void aCustomerWithAnInvalidToken() {
        aCustokentomerWithAToken();
        token = new Token(customerId,"invalid-token-uuid",false);
        sid = "RandomSessionID2";
    }

    @When("a GetCustomerIdFromTokenRequested event is sent")
    public void aGetCustomerIdFromTokenRequestedEventIsSent() throws InterruptedException {
        new Thread(() -> {
            EventResponse eventResponse = new EventResponse(sid, true, null, token.getUuid());
            Event CustomerIdTokenRequestEvent = new Event(GET_CUSTOMER_ID_FROM_TOKEN_REQUEST+sid, new Object[] {eventResponse});
            eventHandler.handleGetCustomerIdFromTokenRequest(CustomerIdTokenRequestEvent);
        }).start();

        Thread.sleep(200);

    }

    @When("the GetCustomerIdFromTokenResponded event is received")
    public void theGetCustomerIdFromTokenRespondedEventIsReceived() {
        final Event event = mq.getEvent(GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE+sid);

        mq.verify(event);
        tokenEventResponse = event.getArgument(0, EventResponse.class);
    }

    @Then("the customerId is obtained from token")
    public void theCustomerIdIsObtainedFromToken() {
        final Token responseToken = tokenEventResponse.getArgument(0, Token.class);
        assertEquals(customerId,responseToken.getCustomerId());
        assertTrue(responseToken.getValidToken());
        assertEquals(token.getUuid(),responseToken.getUuid());
    }

    @And("the token should be invalid")
    public void theTokenShouldBeInvalid() {
        final Event event = mq.getEvent(GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE+sid);

        tokenEventResponse = event.getArgument(0, EventResponse.class);
        final Token responseToken = tokenEventResponse.getArgument(0, Token.class);
        assertFalse(responseToken.getValidToken());
    }

}
