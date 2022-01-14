package dtu.TokenService.Presentation.Resources;

import java.util.concurrent.CompletableFuture;

import dtu.TokenService.Application.TokenService;
import messaging.Event;
import messaging.MessageQueue;
import dtu.TokenService.Domain.Entities.Token;

public class TokenMessageService {
	
	private MessageQueue messageQueue;
	private CompletableFuture<Boolean> customerVerified;
	private TokenService tokenService;

	public TokenMessageService(MessageQueue messageQueue, TokenService tokenService) {
		this.messageQueue = messageQueue;
		this.tokenService = tokenService;
		this.messageQueue.addHandler("TokenVerificationRequested", this::handleTokenVerificationRequest);
		this.messageQueue.addHandler("CustomerVerified", this::handleCustomerVerification);
	}


	// We send a verification request meant for AccountService with the customerId
	public Boolean verifyCustomer(String customerId) {
		customerVerified = new CompletableFuture<>();
		Event event = new Event("CustomerVerificationRequested", new Object[] { customerId });
		messageQueue.publish(event);
		return customerVerified.join();
	}

	// Handler for verification response from AccountService in the form of a boolean to see if the customer is registered.
	public void handleCustomerVerification(Event e) {
		var s = e.getArgument(0, Boolean.class);
		customerVerified.complete(s);
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// Handler for verification request from Payments that needs to know if the token is valid and the cid for the token.
	public void handleTokenVerificationRequest(Event e) {
		var tokenUuid = e.getArgument(0, String.class);
		Token tokenObj = tokenService.getVerifiedToken(tokenUuid);
		Event event = new Event("TokenVerificationResponse", new Object[] { tokenObj });
		messageQueue.publish(event);
	}
}
