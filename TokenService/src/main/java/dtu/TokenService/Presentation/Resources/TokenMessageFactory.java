package dtu.TokenService.Presentation.Resources;

import dtu.TokenService.Application.TokenService;
import dtu.TokenService.Domain.Repositories.LocalTokenRepository;
import messaging.implementations.RabbitMqQueue;

public class TokenMessageFactory {
  
  static TokenMessageService service = null;

	public TokenMessageService getService() {
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
		var messageQueue = new RabbitMqQueue("rabbitMq");

		//TODO: Check how to add busniss logic here.
		
		service = new TokenMessageService(messageQueue, new TokenService(new LocalTokenRepository()));
    //new StudentRegistrationServiceAdapter(service, mq);
		return service;
  }
}
