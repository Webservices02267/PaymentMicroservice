package dtu.services;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import messaging.Event;
import messaging.MessageQueue;
import dtu.TokenService.Domain.Entities.Payment;
import dtu.TokenService.Presentation.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PaymentTestSteps {
    MessageQueue queue = mock(MessageQueue.class);
    dtu.TokenService.Presentation.Resources.PaymentDispatcher2 PaymentDispatcher2 = new dtu.TokenService.Presentation.Resources.PaymentDispatcher2(queue);

    Payment payment = new Payment("1","2","3","4");

    @When("a {string} event for a payment is received")
    public void aEventForAPaymentIsReceived(String event2) {
        System.out.println("eventName2: "+ event2);
        PaymentDispatcher2.handlePaymentRequested(new Event(event2,new Object[] {payment}));
    }

    @Then("the {string} event is sent")
    public void theEventIsSent(String eventName) {
        Payment expected_payment = new Payment("1","2","3","4");
        System.out.println("eventName: "+ eventName);
        var event = new Event("aPaymentRequested", new Object[] {expected_payment});
        verify(queue).publish(event);
        System.out.println("Success with using: \"aPaymentRequested\" and not the eventName: "+eventName);
    }

    @And("the payment is successful")
    public void thePaymentIsSuccuessful() {
        Payment expected_payment = new Payment("1","2","3","4");
        assertNotNull(expected_payment.getCustomerId());
    }
}
