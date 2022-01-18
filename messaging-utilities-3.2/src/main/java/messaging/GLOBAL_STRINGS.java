package messaging;

import java.math.BigDecimal;

public class GLOBAL_STRINGS {
    public static class OK_STRINGS{
        public static final String ALL_GOOD= "All good";
    }
    public static class TOKEN_SERVICE{
        public static class OK_STRINGS{
            public static final String SANITITY_CHECK_FOR_ACCOUNT_SERVICE = "Sanitity check for account service";
        }

        public static class HANDLE {

            public static final String ACCOUNT_STATUS_REQUEST = "AccountStatusRequest";
            public static final String GET_CUSTOMER = "GetCustomer";
            public static final String GET_MERCHANT = "GetMerchant";
            public static final String CUSTOMER_VERIFICATION_REQUESTED = "CustomerVerificationRequested";
            public static final String MERCHANT_VERIFICATION_REQUESTED = "MerchantVerificationRequested";
            public static final String CUSTOMER_CREATION_REQUESTED = "CustomerCreationRequested";
            public static final String MERCHANT_CREATION_REQUESTED = "MerchantCreationRequested";
            public static final String MERCHANT_ID_TO_ACCOUNT_NUMBER_REQUEST = "MerchantIdToAccountNumberRequest";
            public static final String CUSTOMER_ID_TO_ACCOUNT_NUMBER_REQUEST = "CustomerIdToAccountNumberRequest";
        }
        public static class PUBLISH {
            public static final String ACCOUNT_STATUS_RESPONSE = "AccountStatusResponse";
            public static final String CUSTOMER_CREATION_RESPONSE = "CustomerCreationResponse";
            public static final String MERCHANT_CREATION_RESPONSE = "MerchantCreationResponse";
            public static final String CUSTOMER_VERIFICATION_RESPONSE = "CustomerVerificationResponse";
            public static final String MERCHANT_VERIFICATION_RESPONSE = "MerchantVerificationResponse";
            public static final String RESPONSE_CUSTOMER = "ResponseCustomer";
            public static final String RESPONSE_MERCHANT = "ResponseMerchant";
            public static final String MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE = PAYMENT_SERVICE.HANDLE.MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE;
            public static final String CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE = PAYMENT_SERVICE.HANDLE.CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE;


        }
        public static class ERROR_STRINGS{

            public static final String AN_ERROR_HAS_OCCURED_COULD_NOT_CREATE_MERCHANT = "AN ERROR HAS OCCURED - COULD NOT CREATE MERCHANT";
            public static final String AN_ERROR_HAS_OCCURED_COULD_NOT_CREATE_CUSTOMER = "AN ERROR HAS OCCURED - COULD NOT CREATE CUSTOMER";

            public static final String NO_MERCHANT_EXISTS_WITH_THE_PROVIDED_ID = "No merchant exists with the provided id";
            public static final String NO_CUSTOMER_EXISTS_WITH_THE_PROVIDED_ID = "No customer exists with the provided id";

        }
    }
    public static class PAYMENT_SERVICE{


        public static class HANDLE {
            public static final String PAYMENT_STATUS_REQUEST = "PaymentStatusRequest";
            public static final String PAYMENT_REQUEST = "PaymentRequest";
            public static final String MERCHANT_TO_ACCOUNT_NUMBER_RESPONSE = "MerchantIdToAccountNumberResponse";
            public static final String GET_CUSTOMER_ID_FROM_TOKEN_RESPONSE = "GetCustomerIdFromTokenResponse";
            public static final String CUSTOMER_TO_ACCOUNT_NUMBER_RESPONSE = "CustomerIdToAccountNumberResponse";
        }

        public static class PUBLISH {
            public static final String PAYMENT_STATUS_RESPONSE = "PaymentStatusResponse";
            public static final String MERCHANT_TO_ACCOUNT_NUMBER_REQUEST = "MerchantIdToAccountNumberRequest";
            public static final String GET_CUSTOMER_ID_FROM_TOKEN_REQUEST = "GetCustomerIdFromTokenRequest";
            public static final String CUSTOMER_TO_ACCOUNT_NUMBER_REQUEST = "CustomerIdToAccountNumberRequest";
            public static final String PAYMENT_RESPONSE = "PaymentResponse";
        }



        public static class ERROR_STRINGS{
            public static final String DEBTOR_ID_MUST_NOT_BE_NULL= "debtor Id must not be null";
            public static final String CREDITOR_ID_MUST_NOT_BE_NULL= "creditor Id must not be null";
            public static final String AMOUNT_MUST_NOT_BE_NULL= "Amount must not be null";
            public static final String AMOUNT_MUST_BE_A_POSITIVE_NUMBER= "Amount must be a positive number";
            public static final String AMOUNT_MUST_BE_A_NUMBER= "Amount must be a number";
            public static final String TOKEN_MUST_NOT_BE_NULL= "Token must not be null";

            public static final String CREDITOR_ACCOUNT_IS_NOT_VALID= "Creditor account is not valid";
            public static final String DEBTOR_ACCOUNT_IS_NOT_VALID= "Debtor account is not valid";
            public static final String TOKEN_ID_MUST_BE_VALID= "Token must be valid";
            public static final String INSUFFICIENT_BALANCE_ON_DEBTOR_ACCOUNT= "Insufficient balance on debtor account";

        }

    }


}
