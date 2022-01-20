Feature: VerifyPayment
  Scenario: Payment service status
    When the payment service is requested for its status
    Then the status message is "Sanitity check for payment service"


  Scenario: simple payment
    Given a valid Payment Request with sessionid "1"
    When a payment request is published by rest
    Then a payment request is sent
    Then a payment response is verified

  Scenario: payment + Merchant
    Given a valid Payment Request with sessionid "2"
    When a payment request is published by rest
    Then a payment request is sent
    Then a payment response is verified

  Scenario: payment not responding
    Given a valid Payment Request with sessionid "3"
    And no payment request is verified in time
    When a payment request is published by rest
    Then a payment request will return "No response from PaymentService"
