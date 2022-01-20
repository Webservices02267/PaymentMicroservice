package dtu.services;


import dtu.application.BankServiceWrapper;
import dtu.application.IPaymentService;
import dtu.application.PaymentServiceImplementation;
import dtu.application.interfaces.*;
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
import messaging.GLOBAL_STRINGS;
import messaging.MessageQueue;



import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.PUBLISH.PAYMENT_RESPONDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PaymentRequestHandleSteps {
    BankService bankService = new MockBankService();
    BankServiceWrapper bankServiceWrapper = new BankServiceWrapper(bankService);

    IAccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentServiceImplementation(bankService, new InMemoryRepository());
    MockTokenService tokenService = new MockTokenService();
    MessageQueue messageQueue = mock(MessageQueue.class);
    PaymentEventHandler service = new PaymentEventHandler(messageQueue, paymentService);
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

    @After
    public void removeCustomerAndMerchantFromBank() {
        try {
            bankServiceWrapper.retireAccount(customerId);
            bankServiceWrapper.retireAccount(merchantId);
        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
    }


    @Given("a valid PaymentRequest")
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

        payment = new PaymentDTO(merchantId, token.getUuid(), amount);

        payment.description = "this is cucumber";
    }


    //the paymentevent "PaymentRequest" is sent is sent from restService
    @When("the paymentevent {string} is sent from restService")
    public void theEventIsSent(String event_name) {
        AtomicReference<Event> e = null;
        new Thread(() -> {
            //Start a payment from the restService and wait for the PaymentResponse
            assert false;
            e.set(service.doPaymentRequestEvent(payment, "1"));
            //Check if the payment was successful or not
            final Boolean join = paymentAttempt.join();
            assertEquals(successfulPayment,join);

        }).start();

        //Avoid Race Condition when on linux
        try{Thread.sleep(50);}catch(InterruptedException ee){System.out.println(ee);}

    }
    //Then the paymentevent "PaymentRequest" is received
    @Then("the paymentevent {string} is received")
    public void theEventIsReceived(String event_name) {
        //Verify that the "PaymentRequest" is recieved
        Event e = new Event(event_name, new Object[] { payment });
        verify(messageQueue).publish(e);

        //Then we handle the paymentRequest and send the "TokenVerificationRequest"
        service.handlePaymentRequest(e);
    }
    //And the tokenevent "TokenVerificationRequest" is published
    @And("the tokenevent {string} is published")
    public void theEventIsPublishedWithToken(String event_name) {
        //assertEquals(payment.token , token.getUuid());
        Event e = new Event(event_name, new Object[] { payment.token });
        verify(messageQueue).publish(e);
    }
    Event tokenEvent;
    // the tokenevent "TokenVerificationResponse" is sent from tokenService
    @When("the tokenevent {string} is sent from tokenService")
    public void theEventIsSentFromTokenService(String event_name) {
        //final boolean valid_token = service.doGetCustomerIdFromTokenResponse(new Event(event_name, new Object[] {token})).getArgument(0, Token.class).getValidToken();
        //TODO: Make sure that the token to payment mapping is saved in the payment service between calls
        //Correlation between payment -> token
        //tokenEvent = new Event(event_name, new Object[] { new Token(customerId, payment.token, valid_token) });
        //verify(messageQueue).publish(tokenEvent);
    }

    @And("the customerid is no longer valid in the bank")
    public void theCustomeridIsNoLongerValidInTheBank() {
        final boolean valid_token =tokenEvent.getArgument(0,Token.class).getValidToken();
        valid_payment = false;
        customerId = "1"; //'Account ids' in the bank are UUIDs, so sending a '1' should always be invalid
        //This simulates that our token service has a token->'customer id', that is no longer used in the SOAP bank
        tokenEvent = new Event(tokenEvent.getType(), new Object[] { new Token(customerId, payment.token, valid_token) });
    }


    Event bank_result;
    // the tokenevent "TokenVerificationResponse" is received
    @Then("the tokenevent {string} is received")
    public void theTokeneventIsReceived(String event_name) {
        //Type and topic is the same in this version of Huberts messaging util
        assertEquals(event_name,tokenEvent.getType());

        //bank_result = service.doCustomerIdToAccountNumberResponse(tokenEvent);
    }


    //the paymentevent "PaymentResponse" is published
    @Then("the paymentevent {string} is published")
    public void thePaymenteventIsPublished(String event_name) {

        successfulPayment= bank_result.getArgument(0, Boolean.class);
        String return_str= bank_result.getArgument(1, String.class);

        //Check if the type/topic is a "PaymentResponse" event
        assertEquals(bank_result.getType(), event_name);


        Event event;
        if(successfulPayment){
            event = new Event(PAYMENT_RESPONDED, new Object[] {true, return_str });
        }else{
            event = new Event(PAYMENT_RESPONDED, new Object[] {false, return_str });
            errorMessage = return_str;
        }
        verify(messageQueue).publish(event);
        paymentAttempt.complete(successfulPayment);

        System.out.println("A payment was done with the response: \""
                +valid_payment+"\" and return str: \""+return_str+"\"");
    }

    @And("the payment transfer failed")
    public void thePaymentTransferFailed() {
        assertFalse(successfulPayment);
    }
    @And("the payment transfer succeeded")
    public void thePaymentSucceeded() {
        assertTrue(successfulPayment);
    }

    @And("the error message from the transfer is {string}")
    public void theErrorMessageIs(String message) {
        assertEquals(message, errorMessage);
    }
}
