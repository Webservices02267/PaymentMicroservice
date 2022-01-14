package dtu.services;


import dtu.TokenService.Application.repos.LocalPaymentRepository;
import dtu.TokenService.Application.services.BankServiceWrapper;
import dtu.TokenService.Application.services.MockAccountService;
import dtu.TokenService.Application.services.MockBankService;
import dtu.TokenService.Application.services.MockReportService;
import dtu.TokenService.Application.services.MockTokenService;
import dtu.TokenService.Application.services.PaymentService;
import dtu.TokenService.Application.services.interfaces.AccountService;
import dtu.TokenService.Application.services.interfaces.IPaymentService;
import dtu.TokenService.Application.services.interfaces.LogService;
import dtu.TokenService.Application.services.interfaces.ReportService;
import dtu.TokenService.Application.services.interfaces.TokenService;
import dtu.TokenService.Presentation.Resources.PaymentDispatcher;
import dtu.TokenService.Presentation.Resources.RabbitmqStrings;
import dtu.ws.fastmoney.BankService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import messaging.Event;
import messaging.MessageQueue;

import java.util.concurrent.CompletableFuture;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PaymentQueueSteps {


    BankService bs = new MockBankService();
    BankServiceWrapper bankService = new BankServiceWrapper(bs);
    AccountService accountService = new MockAccountService();
    IPaymentService paymentService = new PaymentService(new LocalPaymentRepository());
    TokenService tokenService = new MockTokenService();
    LogService logService;
    ReportService reportService = new MockReportService();
    String customerId;
    String merchantId;
    String token;
    private MessageQueue messageQueue = mock(MessageQueue.class);
    private PaymentDispatcher service = new PaymentDispatcher(messageQueue, paymentService);
    private CompletableFuture<String> tokenRequested = new CompletableFuture<>();
    private CompletableFuture<String> tokenResponed = new CompletableFuture<>();

    @Given("a token {string}")
    public void aToken(String token) {
        this.token = token;
    }

    @When("the token is being verified")
    public void theTokenIsBeingVerified() {
        new Thread(() -> {
            var result = service.generateVerifyTokenRequest(token);
            tokenRequested.complete(result);
        }).start();

    }

    @Then("the event {string} is published")
    public void theEventIsPublished(String event) {

        Event e = new Event(event, new Object[] { token });
        verify(messageQueue).publish(e);

        new Thread(() -> {
            var result = service.doVericationOfToken(token);
            tokenResponed.complete(result);
        }).start();
        //Actually do the verification of the token

        assertNotNull(tokenRequested.join());

    }

    @When("the event {string} is received with customerId and token {string}")
    public void theEventIsReceivedWithCustomerIdAndToken(String event, String token) {
        assertEquals(this.token,token);
        Event e = new Event(event, new Object[] { token });
        verify(messageQueue).publish(e);
        assertEquals(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE,event);
        //service.handleTokenVerificationResponse(new Event(event, new Object[] {token}));

    }
    @Then("the token is verified")
    public void theTokenIsVerified() {
        service.complete_token_verified(token);
    }


}
