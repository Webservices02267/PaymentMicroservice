Feature: Verify customer

  Scenario: Merchant verification
  # Create Merchant object
    Given a registered Merchant with accountNumber "1234"
  # Send request
    When the Merchant is being verified
  # Verificer that it have been sent
    Then the verification request event is sent

  # Reveive the response
    When the verification response event is sent with MerchantId
  # Assert that the response is valid.
    Then the Merchant is verified
