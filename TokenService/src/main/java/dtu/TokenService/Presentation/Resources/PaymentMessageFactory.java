package dtu.TokenService.Presentation.Resources;


import dtu.TokenService.Application.repos.LocalPaymentRepository;
import dtu.TokenService.Application.services.PaymentService;
import messaging.implementations.RabbitMqQueue;

public class PaymentMessageFactory {
    static PaymentDispatcher service = null;

    public PaymentDispatcher getService() {
        // The singleton pattern.
        // Ensure that there is at most
        // one instance of a PaymentService
        if (service != null) {
            return service;
        }

        // Hookup the classes to send and receive
        // messages via RabbitMq, i.e. RabbitMqSender and
        // RabbitMqListener.
        // This should be done in the factory to avoid
        // the PaymentService knowing about them. This
        // is called dependency injection.
        // At the end, we can use the PaymentService in tests
        // without sending actual messages to RabbitMq.
        var messageQueue = new RabbitMqQueue("localhost");

        //TODO: Check how to add busniss logic here.

        service = new PaymentDispatcher(messageQueue, new PaymentService(new LocalPaymentRepository()));
        //new StudentRegistrationServiceAdapter(service, mq);
        return service;
    }
}
