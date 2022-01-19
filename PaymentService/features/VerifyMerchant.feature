Feature: Verify accounts

  Scenario: Merchant verification
  # Create Merchant object
    Given a registered Merchant with accountNumber "1234"
  # Send request
    When the Merchant is being verified
  # Verificer that it have been sent
    Then the "Merchant" verification request event is sent

  # Reveive the response
    When the verification response event is sent with MerchantId
  # Assert that the response is valid.
    Then the Merchant is verified

  Scenario: Customer verification
  # Create Customer object
    Given a registered Customer with accountNumber "12345"
  # Send request
    When the Customer is being verified
  # Verificer that it have been sent
    Then the "Customer" verification request event is sent

  # Reveive the response
    When the verification response event is sent with TokenId
  # Assert that the response is valid.
    Then the Customer is verified


  Scenario: payment not responding
    Given a registered account with accountNumber "1234"
    And no accountVerification request is verified in time
    When the account is being verified
    Then a accountVerification request will return "No response from AccountService"
