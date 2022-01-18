# Payment service

The service is a part of the total DTU Pay microservice system where the **responsibility** of the service is to handle
*paymentRequests*, which can be a normal payment or a refund payment received from the **REST service**.
The payment service receives a *paymentRequest*, which has a merchant id, a token and an amount.

It is then the job of the **Token service** to verify the token and return a customer id that corresponds to the token.
A transfer between DTU Pay and the SOAP bank will then happen with the merchant id, the customer id and the amount. 
The bank will return the response to the transfer and a *paymentResponse* will be given back to the 
**REST service** that asked.

All *xRequests* and *xResponse* are events that are happening on the RabbitMQ queue.

The transfer between DTU Pay and the SOAP bank is done with a SOAP call.

The **REST Service** receives queries over REST. 

## Build process

The build process is described with a series of scripts.

To do a complete build and deployment and test call the `./build_and_run.sh` script.

Each of the steps in this file is also described in the Jenkins file. where the steps is called in the order of and these scripts will go down in each project respectively compile/build/package/create docker images then deploy and test the service

- `./build.sh`
- `./deploy.sh`
- `./test.sh`

To stop the service run `./stop.sh`.
