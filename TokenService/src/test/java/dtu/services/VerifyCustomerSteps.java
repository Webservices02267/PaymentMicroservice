package dtu.services;
import static org.junit.Assert.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;

import dtu.TokenService.Application.TokenService;
import dtu.TokenService.Domain.Repositories.LocalTokenRepository;
import dtu.TokenService.Presentation.Resources.TokenMessageService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import messaging.Event;
import messaging.MessageQueue;

public class VerifyCustomerSteps {
	String customerId = null;
	String merchantId = null;

	private MessageQueue messageQueue = mock(MessageQueue.class);
	private TokenService tokenService = new TokenService(new LocalTokenRepository());
	private TokenMessageService messageService = new TokenMessageService(messageQueue, tokenService);
	private CompletableFuture<Boolean> customerVerified = new CompletableFuture<>();

	@Given("a customer has an id {string}")
	public void aCustomerHasAnId(String customerId){
		this.customerId = customerId;
	}

	@When("the customer is being verified")
	public void theCustomerIsBeingVerified() {
		// We have to run the verification in a thread, because
		// the register method will only finish after the next @When
		// step is executed.
		new Thread(() -> {
			var result = messageService.verifyCustomer(customerId);
			customerVerified.complete(result);
		}).start();
	}

	@Then("the {string} event is sent")
	public void theEventIsSent(String sendEventString) {
		Event event = new Event(sendEventString, new Object[] { customerId });
		verify(messageQueue).publish(event);
	}
	
	@When("the {string} event is sent with customerId")
	public void theEventIsSentWithCustomerId(String returnEventString) {
		// This step simulate the event created by a downstream service.'
		boolean returnVal = true;
		messageService.handleCustomerVerification(new Event("..",new Object[] {returnVal}));
	}

	@Then("the customer is verified")
	public void theCustomerIsVerified() {
		// Our logic is very simple at the moment; we don't
		// remember that the student is registered.
		assertNotNull(customerVerified.join().booleanValue());
	}

}