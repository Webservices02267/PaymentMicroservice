package dtu.Domain;



import java.math.BigDecimal;

public class Payment {

    private String merchantId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Payment payment = (Payment) o;

        if (status != payment.status) return false;
        if (merchantId != null ? !merchantId.equals(payment.merchantId) : payment.merchantId != null) return false;
        if (token != null ? !token.equals(payment.token) : payment.token != null) return false;
        if (amount != null ? !amount.equals(payment.amount) : payment.amount != null) return false;
        return customerId != null ? customerId.equals(payment.customerId) : payment.customerId == null;
    }

    @Override
    public int hashCode() {
        int result = merchantId != null ? merchantId.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (status ? 1 : 0);
        return result;
    }

    private String token;

    private String amount;
    private String customerId;
    private boolean status;

    public Payment(String merchantId, String token, String amount) {
        try {
            setMerchantId(merchantId);
            setToken(token);
            setAmount(amount);
        } catch (ArgumentNullException | AmountIsNotANumberException | NegativeAmountException e) {
            e.printStackTrace();
        }
    }

    public Payment(String customerId, String merchantId, String token, String amount) {
        try {
            setCustomerId(customerId);
            setMerchantId(merchantId);
            setToken(token);
            setAmount(amount);
        } catch (ArgumentNullException | AmountIsNotANumberException | NegativeAmountException e) {
            e.printStackTrace();
        }
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public static Payment build(String customerId, String merchantId, String token, String amount) throws ArgumentNullException, NegativeAmountException, AmountIsNotANumberException {
        Payment payment = new Payment();
        payment.setMerchantId(merchantId);
        payment.setToken(token);
        payment.setAmount(amount);
        return payment;
    }

    public Payment() {
    }


    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) throws ArgumentNullException {
        if (merchantId == null) throw new ArgumentNullException("Merchant Id must not be null");
        this.merchantId = merchantId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) throws ArgumentNullException {
        if (token == null) throw new ArgumentNullException("Token must not be null");
        this.token = token;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) throws ArgumentNullException, AmountIsNotANumberException, NegativeAmountException {
        if (amount == null) throw new ArgumentNullException("Amount must not be null");
        try {
            var number = new BigDecimal(amount);
            if (number.compareTo(new BigDecimal("0")) < 0) throw new NegativeAmountException("Amount must be a positive number");
        } catch (NumberFormatException e) {
            throw new AmountIsNotANumberException("Amount must be a number");
        }
        this.amount = amount;
    }


}
