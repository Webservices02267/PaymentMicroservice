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
import dtu.ws.fastmoney.Account;
import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceException_Exception;
import dtu.ws.fastmoney.User;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import messaging.Event;
import messaging.EventResponse;
import messaging.implementations.MockMessageQueue;
import org.junit.Before;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.HANDLE.MERCHANT_VERIFICATION_REQUESTED;
import static messaging.GLOBAL_STRINGS.ACCOUNT_SERVICE.PUBLISH.MERCHANT_VERIFICATION_RESPONSE;
import static org.junit.Assert.assertEquals;
import static dtu.presentation.AccountEventHandler.full_payment_timeout_periode;



public class VerifyAccountSteps {

    private Event MerchantVerificationResponse;

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
    AccountEventHandler service = new AccountEventHandler(mq, accountService);

    CompletableFuture<Boolean> paymentAttempt = new CompletableFuture<>();
    CompletableFuture<Event> merchantVerificationResponseComplete = new CompletableFuture<>();


    PaymentDTO payment;
    private String status;
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


    @When("the Merchant is being verified")
    public void theMerchantIsBeingVerified() {
        new Thread(() -> {
            MerchantVerificationResponse = service.customerVerificationRequest(merchantId, sid);
            merchantVerificationResponseComplete.complete(MerchantVerificationResponse);
        }).start();
    }


    @Then("the verification request event is sent")
    public void theVerificationRequestEventIsSent() throws InterruptedException {
        EventResponse eventResponse = new EventResponse(sid , true, null, merchantId);
        Event event = new Event(MERCHANT_VERIFICATION_REQUESTED, eventResponse);
        Thread.sleep(100); // added feature for concurrency
        assertEquals(event, mq.getEvent(MERCHANT_VERIFICATION_REQUESTED));
    }

    @When("the verification response event is sent with MerchantId")
    public void theVerificationResponseEventIsSentWithMerchantId() {
        // This step simulate the event created by the account service.'
        EventResponse eventResponse = new EventResponse(sid, true, accountNumber);
        Event responseEvent = new Event(MERCHANT_VERIFICATION_RESPONSE+"." + sid, eventResponse);
        service.handleCustomerVerificationResponse(responseEvent);
    }

    @Then("the Merchant is verified")
    public void theMerchantIsVerified() {
        EventResponse eventResponse = new EventResponse(sid, true, accountNumber);
        Event expectedEvent = new Event(MERCHANT_VERIFICATION_RESPONSE+"." + sid, eventResponse);
        Event actualEvent = merchantVerificationResponseComplete.join();
        assertEquals(expectedEvent, actualEvent);
    }


}
