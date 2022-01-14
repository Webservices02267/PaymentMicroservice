Feature: Create tokens feature

  Scenario: Customer requests 5 tokens
    Given a customer with id "1234561234"
    When the customer requests 5 tokens
    Then the customer has 5 tokens
    
  Scenario: Customer with 2 tokens requests 1 tokens
    Given a customer with id "1234561234"
    And the customer already has 2 tokens
    When the customer requests 1 tokens
    Then the customer has 2 tokens
    
  Scenario: Customer with 1 tokens requests 6 tokens
    Given a customer with id "1234561234"
    And the customer already has 1 tokens
    When the customer requests 6 tokens
    Then the customer has 1 tokens
    
  Scenario: Customer with 1 tokens requests -2 tokens
    Given a customer with id "1234561234"
    And the customer already has 1 tokens
    When the customer requests -2 tokens
    Then the customer has 1 tokens

  Scenario: Customer with 1 tokens requests 5 tokens
    Given a customer with id "1234561234"
    And the customer already has 1 tokens
    When the customer requests 5 tokens
    Then the customer has 6 tokens
  
  Scenario: Customer with 0 tokens requests 5 tokens
    Given a customer with id "1234561234"
    And the customer already has 0 tokens
    When the customer requests 5 tokens
    Then the customer has 5 tokens