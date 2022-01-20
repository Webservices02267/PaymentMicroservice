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
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.PAYMENT_REQUESTED;
import static org.junit.Assert.*;

public class PaymentEventSteps {

    BankService bankService = new MockBankService();
    BankServiceWrapper bankServiceWrapper = new BankServiceWrapper(bankService);

    IAccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentServiceImplementation(bankService, new InMemoryRepository());
    MockTokenService tokenService = new MockTokenService();
    MockMessageQueue mq = new MockMessageQueue();
    PaymentEventHandler service = new PaymentEventHandler(mq, paymentService);

    CompletableFuture<Boolean> paymentAttempt = new CompletableFuture<>();
    CompletableFuture<Event> paymentRequestComplete = new CompletableFuture<>();


    PaymentDTO payment;
    private String status;
    String customerId;
    String merchantId;
    Token token;
    String sid;
    String balance;
    String amount;
    boolean valid_payment;

    @Before
    public void beforeStatement() {
        //Make sure that we have 5 seconds timeout periode when doing a payment
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

    @Given("a valid Payment Request with sessionid {string}")
    public void aValidPaymentRequestWithSessionid(String session_id){
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

        sid = session_id;

    }

    @And("no payment request is verified in time")
    public void noPaymentRequestIsVerifiedInTime() {
        //Make the scenario "payment not responding" quick by not waiting 5 seconds
        full_payment_timeout_periode = 100;
    }

    @When("a payment request is published by rest")
    public void aPaymentRequestIsPublishedByRest() {

        new Thread(() -> {
            //Start a payment from the restService and wait for the PaymentResponse
            final Event PaymentResponse = service.doPaymentRequestEvent2(payment, sid);
            paymentRequestComplete.complete(PaymentResponse);

        }).start();

        //Avoid Race Condition when on linux
        try{Thread.sleep(200);}catch(InterruptedException ee){System.out.println(ee);}
    }

    @Then("a payment request is sent")
    public void aPaymentRequestIsSent() throws InterruptedException {
        paymentAttempt.complete(true);
        EventResponse eventResponse = new EventResponse(sid, true, null, payment);
        Event event = new Event(PAYMENT_REQUESTED+sid, eventResponse );
        service.handlePaymentRequest2(event);
        Thread.sleep(200); // added feature for concurrency
        final Event paymentRequest = mq.getEvent(PAYMENT_REQUESTED+sid);
        assertEquals(event,paymentRequest );
    }

    @Then("a payment response is verified")
    public void aPaymentRequestIsVerified() {
        EventResponse eventResponse = paymentRequestComplete.join().getArgument(0, EventResponse.class);
        assertTrue(eventResponse.isSuccess());
    }

    @When("the payment service is requested for its status")
    public void thePaymentServiceIsRequestedForItsStatus() {
        this.status = paymentService.getStatus();
    }

    @Then("the status message is {string}")
    public void theStatusMessageIs(String expectedStatus) {
        assertEquals(this.status, expectedStatus);
    }


    @Then("a payment request will return {string}")
    public void aPaymentRequestWillReturn(String timeout_string) {
        EventResponse eventResponse = paymentRequestComplete.join().getArgument(0, EventResponse.class);
        assertFalse(eventResponse.isSuccess());
        final String error_message = eventResponse.getErrorMessage();
        assertEquals(timeout_string,error_message);
    }


}
