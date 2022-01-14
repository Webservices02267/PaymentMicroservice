package dtu.TokenService.Presentation.Resources;


import dtu.TokenService.Domain.Entities.Payment;
import messaging.Event;
import messaging.MessageQueue;

public class PaymentDispatcher2 {
    MessageQueue queue;

    public PaymentDispatcher2(MessageQueue queue) {
        this.queue = queue;
        this.queue.addHandler("aPaymentRequested", this::handlePaymentRequested);
    }

    public void handlePaymentRequested(Event event) {
        var s = event.getArgument(0, Payment.class);
        Event e = new Event("PaymentResponse", new Object[] { s });
        queue.publish(event);
    }
}
