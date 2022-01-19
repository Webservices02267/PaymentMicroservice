Feature: VerifyPayment
  Scenario: Payment service status
    When the payment service is requested for its status
    Then the status message is "Sanitity check for payment service"


  Scenario: Full payment
    Given a valid Payment Request2
    When a payment request is published by rest
    Then a payment request is sent
    Then a payment request is verified
