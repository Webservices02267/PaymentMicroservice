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
import dtu.presentation.AccountEventHandler;
import dtu.presentation.PaymentDTO;
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

import static dtu.presentation.AccountEventHandler.full_payment_timeout_periode;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.HANDLE.CUSTOMER_VERIFICATION_REQUESTED;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.HANDLE.MERCHANT_VERIFICATION_REQUESTED;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.PUBLISH.CUSTOMER_VERIFICATION_RESPONSE;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.PUBLISH.MERCHANT_VERIFICATION_RESPONSE;
import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.PAYMENT_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class VerifyAccountSteps {

    private Event MerchantVerificationResponse;
    private Event CustomerVerificationResponse;
    private String customerId;

    public VerifyAccountSteps() {
        service.tokenService = tokenService;
        service.accountService = accountService;
    }

    BankService bankService = new MockBankService();

    BankServiceWrapper bankServiceWrapper = new BankServiceWrapper(bankService);
    IAccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentServiceImplementation(bankService, new InMemoryRepository());
    MockTokenService tokenService = new MockTokenService();
    MockMessageQueue mq = new MockMessageQueue();
    AccountEventHandler service = new AccountEventHandler(mq,accountService);

    CompletableFuture<Event> customerVerificationResponseComplete = new CompletableFuture<>();

    String merchantId;
    String accountNumber;
    String sid;
    String balance = "100";

    @Before
    public void beforeStatement() {
        //Make sure that we have 5 seconds timeout periode when doing an account Verification
        full_payment_timeout_periode = 5000;
    }

    @After
    public void afterStatement() {
        try {
            bankServiceWrapper.retireAccount(merchantId);
        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
    }

    @Given("a registered account with accountNumber {string}")
    public void aRegisteredAccountWithAccountNumber(String accountNumber) {
        aRegisteredMerchantWithAccountNumber(accountNumber);
        aRegisteredCustomerWithAccountNumber(accountNumber);
    }




    @Given("a registered Merchant with accountNumber {string}")
    public void aRegisteredMerchantWithAccountNumber(String accountNumber) {

        sid = "UniqueSessionId";

        var merchant = new User();
        merchant.setCprNumber("135413-0505");
        merchant.setLastName("merchant");
        merchant.setFirstName("merchant");

        try {
            merchantId = bankServiceWrapper.createAccountWithBalance(merchant, new BigDecimal(balance));

        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
        accountService.registerMerchant(merchantId);
        this.accountNumber = accountNumber;


    }
    @And("no accountVerification request is verified in time")
    public void noAccountVerificationRequestIsVerifiedInTime() {
        //Make the scenario "payment not responding" quick by not waiting 5 seconds
        full_payment_timeout_periode = 100;
    }



    @When("the Merchant is being verified")
    public void theMerchantIsBeingVerified() {
        new Thread(() -> {
            MerchantVerificationResponse = service.merchantVerificationRequest(merchantId, sid);
            customerVerificationResponseComplete.complete(MerchantVerificationResponse);
        }).start();
        //Avoid Race Condition when on linux
        try{Thread.sleep(200);}catch(InterruptedException ee){System.out.println(ee);}
    }


    @Then("the {string} verification request event is sent")
    public void theVerificationRequestEventIsSent(String accountType) throws InterruptedException {
        EventResponse eventResponse;
        Event event;
        Event expected_event;
        if(accountType.equals("Merchant")){
            eventResponse = new EventResponse(sid , true, null, merchantId);
            event = new Event(MERCHANT_VERIFICATION_REQUESTED+"."+sid, eventResponse);
            expected_event = mq.getEvent(MERCHANT_VERIFICATION_REQUESTED + "." + sid);
        }else{
            eventResponse = new EventResponse(sid , true, null, customerId);
            event = new Event(CUSTOMER_VERIFICATION_REQUESTED+"."+sid, eventResponse);
            expected_event = mq.getEvent(CUSTOMER_VERIFICATION_REQUESTED + "." + sid);
        }

        assertEquals(event,expected_event);
    }

     @When("the verification response event is sent with MerchantId")
    public void theVerificationResponseEventIsSentWithMerchantId() {
        // This step simulate the event created by the account service.'
        EventResponse eventResponse = new EventResponse(sid, true, accountNumber);
        Event responseEvent = new Event(MERCHANT_VERIFICATION_RESPONSE+"." + sid, eventResponse);
        service.handleMerchantVerificationResponse(responseEvent);
    }

    @Then("the Merchant is verified")
    public void theMerchantIsVerified() {
        EventResponse eventResponse = new EventResponse(sid, true, accountNumber);
        Event expectedEvent = new Event(MERCHANT_VERIFICATION_RESPONSE+"." + sid, eventResponse);
        Event actualEvent = customerVerificationResponseComplete.join();
        assertEquals(expectedEvent, actualEvent);
    }

    @Then("a merchantVerification request will return {string}")
    public void aMerchantVerificationRequestWillReturn(String timeout_string) {
        EventResponse eventResponse = customerVerificationResponseComplete.join().getArgument(0, EventResponse.class);
        assertFalse(eventResponse.isSuccess());
        final String error_message = eventResponse.getErrorMessage();
        assertEquals(timeout_string,error_message);
    }


//    For the tokenServiceTest
//    @Given("a registered Customer with accountNumber {string}")
//    public void aRegisteredCustomerWithAccountNumber(String accountNumber) {
//        sid = "accountNumber";
//
//        balance = "1000";
//        var user = new User();
//        user.setCprNumber("135643-1337");
//        user.setLastName("customer");
//        user.setFirstName("customer");
//
//        try {
//            customerId = bankServiceWrapper.createAccountWithBalance(user, new BigDecimal(balance));
//
//        } catch (BankServiceException_Exception e) {
//            e.printStackTrace();
//        }
//        accountService.registerCustomer(customerId);
//        token = tokenService.createTokens(customerId, 5).stream().findFirst().get();
//        accountService.registerMerchant(merchantId);
//
//        this.accountNumber = accountNumber;
//    }


    @Given("a registered Customer with accountNumber {string}")
    public void aRegisteredCustomerWithAccountNumber(String accountNumber) {
        sid = "accountNumber";

        balance = "1000";
        var user = new User();
        user.setCprNumber("135643-1337");
        user.setLastName("customer");
        user.setFirstName("customer");

        try {
            customerId = bankServiceWrapper.createAccountWithBalance(user, new BigDecimal(balance));

        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
        accountService.registerCustomer(customerId);
        accountService.registerMerchant(merchantId);

        this.accountNumber = accountNumber;
    }

    @When("the Customer is being verified")
    public void theCustomerIsBeingVerified() {
        new Thread(() -> {
            CustomerVerificationResponse = service.customerVerificationRequest(customerId, sid);
            customerVerificationResponseComplete.complete(CustomerVerificationResponse);
        }).start();
        //Avoid Race Condition when on linux
        try{Thread.sleep(200);}catch(InterruptedException ee){System.out.println(ee);}

    }

    @When("the verification response event is sent with CustomerId")
    public void theVerificationResponseEventIsSentWithCustomerId() throws InterruptedException {
        // This step simulate the event created by the account service.'
        EventResponse eventResponse = new EventResponse(sid, true, accountNumber);
        Event responseEvent = new Event(CUSTOMER_VERIFICATION_RESPONSE+"." + sid, eventResponse);
        service.handleCustomerVerificationResponse(responseEvent);

    }

    @Then("the Customer is verified")
    public void theCustomerIsVerified() {
        EventResponse eventResponse = new EventResponse(sid, true, accountNumber);
        Event expectedEvent = new Event(CUSTOMER_VERIFICATION_RESPONSE+"." + sid, eventResponse);
        Event actualEvent = customerVerificationResponseComplete.join();
        assertEquals(expectedEvent, actualEvent);
    }





    @When("the account is being verified")
    public void theAccountIsBeingVerified() {
        new Thread(() -> {
            CustomerVerificationResponse = service.customerVerificationRequest(customerId, sid);
            customerVerificationResponseComplete.complete(CustomerVerificationResponse);
        }).start();
        //Avoid Race Condition when on linux
        try{Thread.sleep(200);}catch(InterruptedException ee){System.out.println(ee);}

    }

    @Then("a accountVerification request will return {string}")
    public void aAccountVerificationRequestWillReturn(String timeout_string) {
        EventResponse eventResponse = customerVerificationResponseComplete.join().getArgument(0, EventResponse.class);
        assertFalse(eventResponse.isSuccess());
        final String error_message = eventResponse.getErrorMessage();
        assertEquals(timeout_string,error_message);
    }


}
