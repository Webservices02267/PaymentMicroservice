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
import messaging.GLOBAL_STRINGS;
import messaging.MessageQueue;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FullPaymentEventSteps {

    BankService bankService = new MockBankService();
    BankServiceWrapper bankServiceWrapper = new BankServiceWrapper(bankService);

    IAccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentServiceImplementation(bankService, new InMemoryRepository());
    MockTokenService tokenService = new MockTokenService();
    MessageQueue messageQueue = mock(MessageQueue.class);
    PaymentEventHandler service = new PaymentEventHandler(messageQueue, paymentService);
    public FullPaymentEventSteps() {
        service.tokenService = tokenService;
        service.accountService = accountService;
    }
    CompletableFuture<Boolean> paymentAttempt = new CompletableFuture<>();

    PaymentDTO payment;

    String customerId;
    String merchantId;
    Token token;
    String balance;
    String amount;
    boolean valid_payment;
    private String errorMessage;
    private boolean successfulPayment = true;;


    Event validPaymentRequestEvent = null;
    Event merchantIdToAccountNumberResponseEvent = null;
    Event getCustomerIdToAccountNumberResponseEvent = null;
    Event customerIdToAccountNumberResponseEvent = null;
    Event paymentResponseEvent = null;


    @After
    public void removeCustomerAndMerchantFromBank() {
        try {
            bankServiceWrapper.retireAccount(customerId);
            bankServiceWrapper.retireAccount(merchantId);
        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
    }

    String sid;


    @Given("a valid Payment Request")
    public void aValidPaymentRequest() {
        valid_payment = true;

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
        token = tokenService.createTokens(customerId, 5).stream().findFirst().get();
        accountService.registerMerchant(merchantId);




    }

    @When("a payment request is published")
    public void aPaymentRequestIsPublished() {
        sid = UUID.randomUUID().toString();
        payment = new PaymentDTO(merchantId, token.getUuid(), amount);
        payment.sessionId = sid;
        payment.description = "all your base are belong to cucumber";
        validPaymentRequestEvent = service.doPaymentRequestEvent(payment, sid);
    }

    @Then("a payment request is handled")
    public void aPaymentRequestIsHandled() {
        verify(messageQueue).publish(validPaymentRequestEvent);
        service.handlePaymentRequest(validPaymentRequestEvent);
    }

    @And("a merchant id to account number request is published")
    public void aMerchantIdToAccountNumberRequestIsPublished() {
        assertTrue(service.sessions.get(sid).publishedEvents.containsKey(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.MERCHANT_ID_TO_ACCOUNT_NUMBER_REQUEST));
    }

    @When("a merchant id to account number response is published by account service")
    public void aMerchantIdToAccountNumberResponseIsPublishedByAccountService() {
        merchantIdToAccountNumberResponseEvent = service.doMerchantIdToAccountNumberResponse(merchantId, sid);
    }

    @Then("a merchant id to account number response is handled")
    public void aMerchantIdToAccountNumberResponseIsHandled() {
        verify(messageQueue).publish(merchantIdToAccountNumberResponseEvent);
        service.handleMerchantIdToAccountNumberResponse(merchantIdToAccountNumberResponseEvent);
    }

    @And("a get customer id from token request is published")
    public void aGetCustomerIdFromTokenRequestIsPublished() {
        assertTrue(service.sessions.get(sid).publishedEvents.containsKey(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.GET_CUSTOMER_ID_FROM_TOKEN_REQUEST));
    }

    @When("a get customer id from token response is published by token service")
    public void aGetCustomerIdFromTokenResponseIsPublishedByTokenService() {
        getCustomerIdToAccountNumberResponseEvent = service.doGetCustomerIdFromTokenResponse(token, sid);
    }

    @Then("a get customer id from token response is handled")
    public void aGetCustomerIdFromTokenResponseIsHandled() {
        verify(messageQueue).publish(getCustomerIdToAccountNumberResponseEvent);
        service.handleGetCustomerIdFromTokenResponse(getCustomerIdToAccountNumberResponseEvent);
    }

    @And("a customer id to account number request is published")
    public void aCustomerIdToAccountNumberRequestIsPublished() {
        var publishedEvents = service.sessions.get(sid).publishedEvents;
        assertTrue(publishedEvents.containsKey(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.CUSTOMER_ID_TO_ACCOUNT_NUMBER_REQUEST));
    }

    @When("a customer id to account number response is published by account service")
    public void aCustomerIdToAccountNumberResponseIsPublishedByAccountService() {
        customerIdToAccountNumberResponseEvent = service.doCustomerIdToAccountNumberResponse(customerId, sid);
    }

    @Then("a customer id to account number response is handled")
    public void aCustomerIdToAccountNumberResponseIsHandled() {
        verify(messageQueue).publish(customerIdToAccountNumberResponseEvent);
        service.handleCustomerIdToAccountNumberResponse(customerIdToAccountNumberResponseEvent);
    }

    @And("a payment response is published")
    public void aPaymentResponseIsPublished() {
        var publishedEvents = service.sessions.get(sid).publishedEvents;
        assertTrue(publishedEvents.containsKey(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE));
        paymentResponseEvent = service.sessions.get(sid).publishedEvents.get(GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONSE);
    }

    @And("the payment response message is {string}")
    public void thePaymentResponseMessageIs(String message) {
        var er = paymentResponseEvent.getArgument(0, EventResponse.class);
        if (er.isSuccess()) {
            assertEquals(message, er.getArgument(0, String.class));
        } else {
            assertEquals(message, er.getErrorMessage());
        }
    }

    @And("merchant id is set to {string} and the payment is invalidated")
    public void merchantIdIsSetToAndThePaymentIsInvalidated(String arg0) {
        merchantId = arg0;

    }

    @And("the merchant id to account number response error message is {string}")
    public void theMerchantIdToAccountNumberResponseErrorMessageIs(String arg0) {
        assertEquals(arg0, merchantIdToAccountNumberResponseEvent.getArgument(0, EventResponse.class).getErrorMessage());
    }

    @And("the customer id to account number response error message is {string}")
    public void theCustomerIdToAccountNumberResponseErrorMessageIs(String arg0) {
        assertEquals(arg0, customerIdToAccountNumberResponseEvent.getArgument(0, EventResponse.class).getErrorMessage());
    }

    @And("customer id is set to {string} and the payment is invalidated")
    public void customerIdIsSetToAndThePaymentIsInvalidated(String arg0) {
        customerId = arg0;

    }

}
