package dtu.presentation;

import messaging.Event;

public class Runner {
    public static void main(String[] args) {
        var handler = new PaymentMessageFactory().getService();
    }
}
