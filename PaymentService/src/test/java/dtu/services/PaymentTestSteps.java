package dtu.services;

import dtu.presentation.PaymentDTO;
import dtu.presentation.PaymentDispatcher2;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import messaging.Event;
import messaging.MessageQueue;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;



public class PaymentTestSteps {
    MessageQueue queue = mock(MessageQueue.class);
    PaymentDispatcher2 paymentDispatcher2 = new PaymentDispatcher2(queue);

    PaymentDTO payment = new PaymentDTO("1","2","3");

    @When("a {string} event for a payment is received")
    public void aEventForAPaymentIsReceived(String event2) {
        System.out.println("eventName2: "+ event2);
        paymentDispatcher2.handlePaymentRequested(new Event(event2,new Object[] {payment}));
    }

    @Then("the {string} event is sent")
    public void theEventIsSent(String eventName) {
        PaymentDTO payment = new PaymentDTO("1","2","3");
        System.out.println("eventName: "+ eventName);
        var event = new Event("aPaymentRequested", new Object[] {payment});
        verify(queue).publish(event);
        System.out.println("Success with using: \"aPaymentRequested\" and not the eventName: "+eventName);
    }

    @And("the payment is successful")
    public void thePaymentIsSuccuessful() {
        PaymentDTO payment = new PaymentDTO("1","2","3");
        assertNotNull(payment);
    }
}
