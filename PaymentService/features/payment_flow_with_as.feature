Feature: Full payment flow
  Scenario: Full payment
    Given a valid Payment Request
    When a payment request is published
    Then a payment request is handled
    And a merchant id to account number request is published
    When a merchant id to account number response is published by account service
    Then a merchant id to account number response is handled
    And a get customer id from token request is published
    When a get customer id from token response is published by token service
    Then a get customer id from token response is handled
    And a customer id to account number request is published
    When a customer id to account number response is published by account service
    Then a customer id to account number response is handled
    And a payment response is published
    And the payment response message is "ok"

  Scenario: Full payment but invalid merchant id
    Given a valid Payment Request
    And merchant id is set to "merchant" and the payment is invalidated
    When a payment request is published
    Then a payment request is handled
    And a merchant id to account number request is published
    When a merchant id to account number response is published by account service
    Then a merchant id to account number response is handled
    And the merchant id to account number response error message is "No merchant exists with the provided id"
    And a payment response is published
    And the payment response message is "Creditor account is not valid"

    @ignore #Beslut om det skal fjernes
    Scenario: Full payment but invalid customer id
      Given a valid Payment Request
      And customer id is set to "customer" and the payment is invalidated
      When a payment request is published
      Then a payment request is handled
      And a merchant id to account number request is published
      When a merchant id to account number response is published by account service
      Then a merchant id to account number response is handled
      And a get customer id from token request is published
      When a get customer id from token response is published by token service
      Then a get customer id from token response is handled
      And a customer id to account number request is published
      When a customer id to account number response is published by account service
      Then a customer id to account number response is handled
      And the customer id to account number response error message is "Debtor account is not valid"
      And a payment response is published
      And the payment response message is "Debtor account is not valid"