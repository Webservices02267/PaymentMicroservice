Feature: Payment via message queues

  Scenario: Token verification
    Given a token "123"
    When the token is being verified
    Then the event "TokenVerificationRequest" is published
    When the event "TokenVerificationResponse" is received with customerId and token "123"
    Then the token is verified
