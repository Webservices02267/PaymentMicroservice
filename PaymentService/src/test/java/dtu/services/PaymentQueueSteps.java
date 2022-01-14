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
import dtu.Presentation.PaymentServiceEventWrapper;
import dtu.Presentation.RabbitmqStrings;
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

    @Then("the event {string} is published2")
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
