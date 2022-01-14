package dtu.Presentation;


import dtu.Domain.Payment;
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
        queue.publish(e);
    }
}
