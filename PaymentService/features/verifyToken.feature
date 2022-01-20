Feature: Get CustomerId from token

  Scenario: A payment is requested with a valid token
    Given A customer with a token
    When a GetCustomerIdFromTokenRequested event is sent
    When the GetCustomerIdFromTokenResponded event is received
    Then the customerId is obtained from token

  Scenario: A payment is requested with an invalid token
    Given A customer with an invalid token
    When a GetCustomerIdFromTokenRequested event is sent
    When the GetCustomerIdFromTokenResponded event is received
    Then the token should be invalid
