Feature: Payments via message queues

  Scenario: Payment request and response happy
    Given a valid PaymentRequest
    #create event and Add event (PaymentRequest) to queue through wrapper
    When the paymentevent "PaymentRequest" is sent is sent from restService
    #Check event (PaymentRequest) is received by handler
    Then the paymentevent "PaymentRequest" is received
    #Check new event (TokenVerificationRequest) has been placed in the queue
    And the tokenevent "TokenVerificationRequest" is published
    #create event and Add event (PaymentRequest) to queue through wrapper
    When the tokenevent "TokenVerificationResponse" is sent from tokenService
    #Check event (TokenVerificationResponse) is received by handler
    Then the tokenevent "TokenVerificationResponse" is received

    ##Here the bank communication should start depending on if the tokens was valid
    # In happy scenario, it is valid
    Then the paymentevent "PaymentResponse" is published

  Scenario: Payment request and response invalid token
    Given an invalid PaymentRequest
    #create event and Add event (PaymentRequest) to queue through wrapper
    When the paymentevent "PaymentRequest" is sent is sent from restService
    #Check event (PaymentRequest) is received by handler
    Then the paymentevent "PaymentRequest" is received
    #Check new event (TokenVerificationRequest) has been placed in the queue
    And the tokenevent "TokenVerificationRequest" is published
    #create event and Add event (PaymentRequest) to queue through wrapper
    When the tokenevent "TokenVerificationResponse" is sent from tokenService
    #Check event (TokenVerificationResponse) is received by handler
    Then the tokenevent "TokenVerificationResponse" is received

    ##Here the bank communication should start depending on if the tokens was valid
    # In happy scenario, it is valid
    Then the paymentevent "PaymentResponse" is published

    #  Scenario: PaymentRequest with invalid customer id from token service (customer has been deleted)
    #  Scenario: PaymentRequest with invalid merchant id at DTUpay
    #  Scenario: PaymentRequest with invalid merchant id (merchant has been deleted/never existed from bank)
    #  Scenario: PaymentRequest with negative amount


    #  Scenario: TokenVerificationResponse with invalid token
    #  Scenario: TokenVerificationResponse with invalid customerid
    #  Scenario: TokenVerificationResponse with valid customerid and token

