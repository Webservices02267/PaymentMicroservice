Feature: Payment

  Scenario: Successful Payment
    Given a customer with a bank account with balance "1000"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the merchant initiates a payment for "100" kr by the customer
    Then the payment succeeded
    And the balance of the customer at the bank is "900" kr
    And the balance of the merchant at the bank is "2100" kr

  Scenario: Customer deleted their bank account after registering for DTU pay
    Given a customer with a bank account with balance "1000"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the customer retires their bank account
    When the merchant initiates a payment for "100" kr by the customer
    Then the payment failed
    And the balance of the merchant at the bank is "2000" kr

  Scenario: Merchant deleted their bank account after registering for DTU pay
    Given a customer with a bank account with balance "1000"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the merchant retires their bank account
    When the merchant initiates a payment for "100" kr by the customer
    Then the payment failed
    And the balance of the customer at the bank is "1000" kr

  Scenario: The token is invalid
    Given a customer with a bank account with balance "1000"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    And the token is invalid
    When the merchant initiates a payment for "100" kr by the customer
    Then the payment failed
    And the error message is "Token must be valid"
    And the balance of the customer at the bank is "1000" kr
    And the balance of the merchant at the bank is "2000" kr

  Scenario: The amount is invalid
    Given a customer with a bank account with balance "1000"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the merchant initiates a payment for "abc" kr by the customer
    Then the payment failed
    And the error message is "Amount must be a number"
    And the balance of the customer at the bank is "1000" kr
    And the balance of the merchant at the bank is "2000" kr

  Scenario: The amount is negative
    Given a customer with a bank account with balance "1000"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the merchant initiates a payment for "-500" kr by the customer
    Then the payment failed
    And the error message is "Amount must be a positive number"
    And the balance of the customer at the bank is "1000" kr
    And the balance of the merchant at the bank is "2000" kr

  Scenario: Insufficient Balance Payment
    Given a customer with a bank account with balance "0"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the merchant initiates a payment for "100" kr by the customer
    Then the payment failed
    And the error message is "Insufficient balance on debtor account"
    And the balance of the customer at the bank is "0" kr
    And the balance of the merchant at the bank is "2000" kr
