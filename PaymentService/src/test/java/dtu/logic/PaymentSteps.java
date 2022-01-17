package dtu.logic;


import dtu.application.BankServiceWrapper;
import dtu.application.PaymentServiceImplementation;
import dtu.application.interfaces.IAccountService;
import dtu.domain.Token;
import dtu.exceptions.*;
import dtu.infrastructure.InMemoryRepository;
import dtu.application.mocks.MockAccountService;
import dtu.application.mocks.MockBankService;
import dtu.application.mocks.MockTokenService;
import dtu.application.IPaymentService;
import dtu.domain.Payment;
import dtu.ws.fastmoney.BankServiceException_Exception;
import dtu.ws.fastmoney.User;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;


import java.math.BigDecimal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PaymentSteps {
 
    BankServiceWrapper bankService = new BankServiceWrapper(new MockBankService());
    IAccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentServiceImplementation(bankService, new InMemoryRepository());
    MockTokenService tokenService = new MockTokenService();
    String customerId;
    String merchantId;
    Token token;
    public boolean successfulPayment = false;
    public String errorMessage;

    @After
    public void removeCustomerAndMerchantFromBank() {
        try {
            bankService.retireAccount(customerId);
            bankService.retireAccount(merchantId);
        } catch (BankServiceException_Exception ignored) {
        }
    }

    @Given("a customer with a bank account with balance {string}")
    public void aCustomerWithABankAccountWithBalance(String balance) {

        var user = new User();
        user.setCprNumber("135643-1337");
        user.setLastName("customer");
        user.setFirstName("customer");
        try {
            customerId = bankService.createAccountWithBalance(user, new BigDecimal(balance));
        } catch (BankServiceException_Exception ignored) {
        }
    }

    @And("that the customer is registered with DTU Pay")
    public void thatTheCustomerIsRegisteredWithDTUPay() {
        accountService.registerCustomer(customerId);
        token = tokenService.createTokens(customerId, 5).stream().findFirst().get();
    }


    @Given("a merchant with a bank account with balance {string}")
    public void aMerchantWithABankAccountWithBalance(String balance) {
        var user = new User();
        user.setCprNumber("135413-0505");
        user.setLastName("merchant");
        user.setFirstName("merchant");
        try {
            merchantId = bankService.createAccountWithBalance(user, new BigDecimal(balance));
        } catch (BankServiceException_Exception ignored) {

        }
    }

    @And("that the merchant is registered with DTU Pay")
    public void thatTheMerchantIsRegisteredWithDTUPay() {
        accountService.registerMerchant(merchantId);
    }


    @And("the balance of the customer at the bank is {string} kr")
    public void theBalanceOfTheCustomerAtTheBankIsKr(String balance) {
        try {
            assertEquals(new BigDecimal(balance), bankService.getAccount(customerId).getBalance());
        } catch (BankServiceException_Exception ignored) {
        }
    }

    @And("the balance of the merchant at the bank is {string} kr")
    public void theBalanceOfTheMerchantAtTheBankIsKr(String balance) {
        try {
            assertEquals(new BigDecimal(balance), bankService.getAccount(merchantId).getBalance());
        } catch (BankServiceException_Exception ignored) {
        }
    }

    @When("the customer retires their bank account")
    public void theCustomerRetiresTheirBankAccount() {
        try {
            bankService.retireAccount(customerId);
        } catch (BankServiceException_Exception ignored) {
        }
    }

    @When("the merchant retires their bank account")
    public void theMerchantRetiresTheirBankAccount() {
        try {
            bankService.retireAccount(merchantId);
        } catch (BankServiceException_Exception ignored) {
        }
    }


    @When("the merchant initiates a payment for {string} kr by the customer")
    public void theMerchantInitiatesAPaymentForKrByTheCustomer(String amount) {
        try {
            successfulPayment = paymentService.pay(new Payment.PaymentBuilder().amount(amount).debtor(customerId).creditor(merchantId).token(token).build());
        } catch (NegativeAmountException | DebtorHasNoBankAccountException | AmountIsNotANumberException | InvalidTokenException | ArgumentNullException | CreditorHasNoBankAccountException | InsufficientBalanceException e) {
            errorMessage = e.getMessage();
            successfulPayment = false;
        }
    }

    @Then("the payment succeeded")
    public void thePaymentSucceeded() {
        assertTrue(successfulPayment);
    }

    @Then("the payment failed")
    public void thePaymentFailed() {
        assertFalse(successfulPayment);
    }

    @And("the error message is {string}")
    public void theErrorMessageIs(String message) {
        assertEquals(message, errorMessage);
    }

    @And("the token is invalid")
    public void theTokenIsInvalid() {
        token.setValidToken(false);
    }

    @Given("a customer with {int} successful payments")
    public void aCustomerWithSuccessfulPayments(int arg0)  {
        aCustomerWithABankAccountWithBalance(String.valueOf(1000*arg0));
        thatTheCustomerIsRegisteredWithDTUPay();
        aMerchantWithABankAccountWithBalance(String.valueOf(0));
        thatTheMerchantIsRegisteredWithDTUPay();
        for (int i = 0; i < arg0; i++) {
            theMerchantInitiatesAPaymentForKrByTheCustomer("1000");
            //TODO: In the correct implementation we receive 5 or 6 tokens
            token = tokenService.createTokens(customerId, 5).stream().findFirst().get();
        }
    }

}
