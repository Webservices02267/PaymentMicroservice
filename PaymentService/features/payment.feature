Feature: Payment

  Scenario: Successful Payment
    Given a customer with a bank account with balance "1000"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the merchant initiates a payment for "100" kr by the customer
    Then the payment is "true" successful
    And the balance of the customer at the bank is "900" kr
    And the balance of the merchant at the bank is "2100" kr

  Scenario: Insufficient Balance Payment
    Given a customer with a bank account with balance "0"
    And that the customer is registered with DTU Pay
    Given a merchant with a bank account with balance "2000"
    And that the merchant is registered with DTU Pay
    When the merchant initiates a payment for "100" kr by the customer
    Then the payment is "false" successful
    And the error message is "Insufficient Balance on customer account"
    And the balance of the customer at the bank is "0" kr
    And the balance of the merchant at the bank is "2000" kr

  Scenario: Customer asks to see a list of their payments
    Given a customer with 5 successful payments
    When the customer asks to see their payments
    Then the payments contains payments between customer and merchant for amount "100"

  Scenario: Merchant asks to see a list of their payments

  Scenario: Manager asks to see a list of their payments