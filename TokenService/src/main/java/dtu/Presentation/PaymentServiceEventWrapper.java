package dtu.Presentation;

import java.util.concurrent.CompletableFuture;

import dtu.Application.IPaymentService;
import dtu.Domain.Payment;
import messaging.Event;
import messaging.MessageQueue;


public class PaymentServiceEventWrapper {

    private final MessageQueue messageQueue;
    private final IPaymentService paymentService;
    private CompletableFuture<Payment> paymentSucceeded;
    CompletableFuture<String> tokenRequest;
    CompletableFuture<String> tokenResponse;


    // TODO: Slet customer Id fra Payment objekt i Resource
    // TODO: Log payment når vi får svar tilbage fra banken.




    public PaymentServiceEventWrapper(MessageQueue messageQueue, IPaymentService paymentService) {
        this.messageQueue = messageQueue;
        this.paymentService = paymentService;
        this.messageQueue.addHandler(RabbitmqStrings.PAYMENT_REQUEST, this::handlePaymentRequest);
        this.messageQueue.addHandler(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, this::handleTokenVerificationResponse);
        this.messageQueue.addHandler(RabbitmqStrings.TOKEN_VERIFICATION_REQUEST, this::handleTokenVerificationRequest);

    }


    /*
    Calls the handler that sends the TokenVerificationRequest
     */
    public String generateVerifyTokenRequest(String token) {
        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_REQUEST, new Object[] { token });
        return handleTokenVerificationRequest(event);
    }


    /*
    Plays the role of token service by handling the request
     */
    private String handleTokenVerificationRequest(Event event) {
        tokenRequest = new CompletableFuture<>();
        System.out.println("handleTokenVerificationRequest publishes: "+event.toString());
        messageQueue.publish(event);
        return tokenRequest.join();
    }

    /*
    Plays the role of the token service by completing the handleTokenVerificationRequest join
     */
    public String doVericationOfToken(String token) {
        Event event = new Event(RabbitmqStrings.TOKEN_VERIFICATION_RESPONSE, new Object[] { token });
        return handleTokenVerificationResponse(event);
        //return
    }

    public void complete_token_verified(String token) {
        tokenResponse.complete(token);
    }


    /**
     * Send PaymentResponse to RestService (valid?,status)
     * if valid token 
     *      do transaction with SOAP Bank and
     *      log the payment in the paymentService with mid,token,amount,cid and status
     * @param event contains cid,token,valid?
     */
    public String handleTokenVerificationResponse(Event event) {
        String token = event.getArgument(0,String.class);
        tokenRequest.complete(token);

        tokenResponse = new CompletableFuture<>();
        System.out.println("handleTokenVerificationResponse publishes: "+event.toString());
        messageQueue.publish(event);
        return tokenResponse.join();


        /*
        if valid token
            do payment with soap bank
            join
        if success send logEvent to logService
        return complete
         */

        //var result = paymentService.pay(payment);

    }



    /**
     * Send tokenVerificationRequest to TokenService (token)
     * @param event contains mid,token and amount
     * @return
     */
    private String handlePaymentRequest(Event event) {
        return "test:handlePaymentRequest";
    }



}
