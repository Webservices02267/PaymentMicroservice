Feature: Verify customer
  
  Scenario: Customer verification
    Given a customer has an id "1234561234"
    When the customer is being verified
    Then the "CustomerVerificationRequested" event is sent
    When the "CustomerVerified" event is sent with customerId
  	Then the customer is verified