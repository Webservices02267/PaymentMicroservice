Feature: Verify token

  Scenario: Token is valid and verified
    Given A customer with id "1234561234"
    When a request to verify the token is received
    Then the token is verified
