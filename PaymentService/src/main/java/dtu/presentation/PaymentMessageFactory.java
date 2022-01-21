package dtu.presentation;


import dtu.application.BankServiceWrapper;
import dtu.application.PaymentServiceImplementation;
import dtu.application.mocks.MockBankService;
import dtu.application.mocks.MockTokenService;
import dtu.infrastructure.InMemoryRepository;
import dtu.ws.fastmoney.BankServiceService;
import messaging.implementations.RabbitMqQueue;

public class PaymentMessageFactory {
    static PaymentEventHandler service = null;

    public PaymentEventHandler getService() {
        // The singleton-pattern.
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

        //TODO: Check how to add business logic here.

        service = new PaymentEventHandler(messageQueue,
                new PaymentServiceImplementation(new BankServiceWrapper
                        (new BankServiceService().getBankServicePort()), new InMemoryRepository()));
        //new StudentRegistrationServiceAdapter(service, mq);
        return service;
    }
}
