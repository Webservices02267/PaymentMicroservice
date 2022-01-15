package dtu.services;


import dtu.application.BankServiceWrapper;
import dtu.application.IPaymentService;
import dtu.application.PaymentServiceImplementation;
import dtu.application.interfaces.*;
import dtu.application.mocks.MockAccountService;
import dtu.application.mocks.MockBankService;
import dtu.application.mocks.MockReportService;
import dtu.application.mocks.MockTokenService;
import dtu.domain.Token;
import dtu.infrastructure.InMemoryRepository;
import dtu.presentation.PaymentDTO;
import dtu.presentation.PaymentServiceEventWrapper2;
import dtu.presentation.RabbitmqStrings;
import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceException_Exception;
import dtu.ws.fastmoney.User;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import messaging.Event;
import messaging.MessageQueue;



import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PaymentRequestHandleSteps {
    BankService bankService = new MockBankService();
    BankServiceWrapper bankServiceWrapper = new BankServiceWrapper(bankService);

    IAccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentServiceImplementation(bankService, new InMemoryRepository());
    MockTokenService tokenService = new MockTokenService();
    MessageQueue messageQueue = mock(MessageQueue.class);
    PaymentServiceEventWrapper2 service = new PaymentServiceEventWrapper2(messageQueue, paymentService,tokenService);
    CompletableFuture<Boolean> paymentAttempt = new CompletableFuture<>();


    PaymentDTO payment;
    String customerId;
    String merchantId;
    Token token;
    String balance;
    String amount;
    boolean valid_payment;

    @After
    public void removeCustomerAndMerchantFromBank() {
        try {
            bankServiceWrapper.retireAccount(customerId);
            bankServiceWrapper.retireAccount(merchantId);
        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
    }

    @Given("an invalid PaymentRequest")
    public void anInvalidPaymentRequest() {
        System.out.println("Doing a valid payment request");
        valid_payment = false;

        balance = "10";
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
    }

    @Given("a valid PaymentRequest")
    public void aValidPaymentRequest() {
        System.out.println("Doing a valid payment request");
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
    }


    //the paymentevent "PaymentRequest" is sent is sent from restService
    @When("the paymentevent {string} is sent is sent from restService")
    public void theEventIsSent(String event_name) {
        Event e = new Event(event_name, new Object[] { payment });
        new Thread(() -> {
            //Start a payment from the restService and wait for the PaymentResponse
            service.doPaymentRequestEvent(e);
            //Check if the payment was successful or not
            assertEquals(valid_payment,paymentAttempt.join());

        }).start();

        //Avoid Race Condition when on linux
        try{Thread.sleep(10);}catch(InterruptedException ee){System.out.println(ee);}

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
        assertEquals(payment.token , token.getUuid());
        Event e = new Event(event_name, new Object[] { payment.token });
        verify(messageQueue).publish(e);
    }

    Event tokenEvent;
    // the tokenevent "TokenVerificationResponse" is sent from tokenService
    @When("the tokenevent {string} is sent from tokenService")
    public void theEventIsSentFromTokenService(String event_name) {
        final boolean valid_token = service.doTokenVerificationResponseEvent(token.getUuid());
        //TODO: Make sure that the token to payment mapping is saved in the payment service between calls
        //Correlation between payment -> token
        tokenEvent = new Event(event_name, new Object[] { new Token(customerId, payment.token, valid_token) });
        verify(messageQueue).publish(tokenEvent);
    }


    Event bank_result;
    // the tokenevent "TokenVerificationResponse" is received
    @Then("the tokenevent {string} is received")
    public void theTokeneventIsReceived(String event_name) {
        //Type and topic is the same in this version of Huberts messaging util
        assertEquals(event_name,tokenEvent.getType());

        bank_result = service.handleTokenVerificationResponse(tokenEvent);
    }

    //the paymentevent "PaymentResponse" is published
    @Then("the paymentevent {string} is published")
    public void thePaymenteventIsPublished(String event_name) {
        Event event;

        boolean aBoolean= bank_result.getArgument(0, Boolean.class);
        String return_str= bank_result.getArgument(1, String.class);

        if(aBoolean){
            event = new Event(RabbitmqStrings.PAYMENT_RESPONSE, new Object[] { aBoolean, return_str });
        }else{
            event = new Event(RabbitmqStrings.PAYMENT_RESPONSE, new Object[] { aBoolean, return_str });
        }
        verify(messageQueue).publish(event);
        paymentAttempt.complete(aBoolean);
        System.out.println("A "+valid_payment+" payment was done");
    }


}
