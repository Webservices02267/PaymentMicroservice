package dtu.services;

import dtu.application.mocks.MockTokenService;
import dtu.domain.Token;
import messaging.Event;
import messaging.EventResponse;
import messaging.implementations.MockMessageQueue;

import static messaging.GLOBAL_STRINGS.PAYMENT_SERVICE.HANDLE.GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE;

public class TokenEventHandler {

    private MockMessageQueue mq;
    private MockTokenService tokenService;

    public TokenEventHandler(MockMessageQueue mq, MockTokenService tokenService) {
        this.mq = mq;
        this.tokenService = tokenService;
    }

    public void handleGetCustomerIdFromTokenRequest(Event customerIdTokenRequestEvent) {
        EventResponse eventArguments = customerIdTokenRequestEvent.getArgument(0, EventResponse.class);
        var sid = eventArguments.getSessionId();
        var token_id = eventArguments.getArgument(0, String.class);
        Token token = tokenService.getVerifiedToken(token_id);
        EventResponse eventResponse = new EventResponse(eventArguments.getSessionId(), token.getValidToken(), null, token);
        Event CustomerIdTokenResponseEvent = new Event(GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE+sid, new Object[] {eventResponse});
        mq.publish(CustomerIdTokenResponseEvent);
    }
}
