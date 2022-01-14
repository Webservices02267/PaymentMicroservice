package dtu.services;


import dtu.Application.BankServiceWrapper;
import dtu.Application.IAccountService;
import dtu.Application.ILogService;
import dtu.Application.IPaymentService;
import dtu.Application.IReportService;
import dtu.Application.ITokenService;
import dtu.Application.LocalPaymentRepository;
import dtu.Application.MockAccountService;
import dtu.Application.MockBankService;
import dtu.Application.MockReportService;
import dtu.Application.MockTokenService;
import dtu.Application.PaymentServiceImplementation;
import dtu.Domain.Payment;
import dtu.Domain.Report;
import dtu.Presentation.PaymentServiceEventWrapper;
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
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PaymentSteps {
 
    BankServiceWrapper bankService = new BankServiceWrapper(new MockBankService());
    IAccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentServiceImplementation(new LocalPaymentRepository());
    ITokenService tokenService = new MockTokenService();
    ILogService logService;
    IReportService reportService = new MockReportService();
    String customerId;
    String merchantId;
    String token;
    private MessageQueue messageQueue = mock(MessageQueue.class);
    private PaymentServiceEventWrapper service = new PaymentServiceEventWrapper(messageQueue, paymentService);
    private CompletableFuture<String> tokenRequested = new CompletableFuture<>();

    @After
    public void removeCustomerAndMerchantFromBank() {
        try {
            bankService.bs.retireAccount(customerId);
            bankService.bs.retireAccount(merchantId);
        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
    }

    @Given("a customer with a bank account with balance {string}")
    public void aCustomerWithABankAccountWithBalance(String balance) {

        var user = new User();
        user.setCprNumber("135643-1337");
        user.setLastName("customer");
        user.setFirstName("customer");
        try {
            customerId = bankService.bs.createAccountWithBalance(user, new BigDecimal(balance));

        } catch (BankServiceException_Exception e) {
            e.printStackTrace();
        }
    }

    @And("that the customer is registered with DTU Pay")
    public void thatTheCustomerIsRegisteredWithDTUPay() {
        accountService.registerCustomer(customerId);
        token = tokenService.getToken(customerId);
    }


    @Given("a merchant with a bank account with balance {string}")
    public void aMerchantWithABankAccountWithBalance(String balance) throws BankServiceException_Exception {
        var user = new User();
        user.setCprNumber("135413-0505");
        user.setLastName("merchant");
        user.setFirstName("merchant");
        merchantId = bankService.bs.createAccountWithBalance(user, new BigDecimal(balance));
    }

    @And("that the merchant is registered with DTU Pay")
    public void thatTheMerchantIsRegisteredWithDTUPay() throws BankServiceException_Exception {
        accountService.registerMerchant(merchantId);
    }


    @And("the balance of the customer at the bank is {string} kr")
    public void theBalanceOfTheCustomerAtTheBankIsKr(String balance) throws BankServiceException_Exception {
        assertEquals(new BigDecimal(balance), bankService.bs.getAccount(customerId).getBalance());
    }

    @And("the balance of the merchant at the bank is {string} kr")
    public void theBalanceOfTheMerchantAtTheBankIsKr(String balance) throws BankServiceException_Exception {
        assertEquals(new BigDecimal(balance), bankService.bs.getAccount(merchantId).getBalance());
    }

    boolean successful = false;
    @When("the merchant initiates a payment for {string} kr by the customer")
    public void theMerchantInitiatesAPaymentForKrByTheCustomer(String amount) {

        try {
            successful = paymentService.pay(new Payment(customerId, merchantId, token, amount));
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }

    }

    @Then("the payment is {string} successful")
    public void thePaymentIsSuccessful(String bool) {
        assertEquals(Boolean.parseBoolean(bool), successful);
    }

    String errorMessage;
    @And("the error message is {string}")
    public void theErrorMessageIs(String message) {
        assertEquals(message, this.errorMessage);
    }

    @Given("a customer with {int} successful payments")
    public void aCustomerWithSuccessfulPayments(int arg0) throws BankServiceException_Exception {
        aCustomerWithABankAccountWithBalance(String.valueOf(1000*arg0));
        thatTheCustomerIsRegisteredWithDTUPay();
        aMerchantWithABankAccountWithBalance(String.valueOf(0));
        thatTheMerchantIsRegisteredWithDTUPay();
        for (int i = 0; i < arg0; i++) {
            theMerchantInitiatesAPaymentForKrByTheCustomer("1000");
            //TODO: In the correct implementation we receive 5 or 6 tokens
            token = tokenService.getToken(customerId);
        }
    }


    @When("the customer asks to see their payments")
    public void theCustomerAsksToSeeTheirPayments() {
       
    }

    @Then("the payments contains payments between customer and merchant for amount {string}")
    public void thePaymentsContainsPaymentsBetweenCustomerAndMerchantForAmount(String arg0) {


    }

    @When("the merchant initiates a payment for {string} kr by the customer message service")
    public void theMerchantInitiatesAPaymentForKrByTheCustomerMessageService(String arg0) {
    }



    @When("when the {string} is received with customerId and token")
    public void whenTheIsReceivedWithCustomerIdAndToken(String arg0) {
    }

}
