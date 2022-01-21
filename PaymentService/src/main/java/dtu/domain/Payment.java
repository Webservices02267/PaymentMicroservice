package dtu.domain;



import dtu.exceptions.AmountIsNotANumberException;
import dtu.exceptions.ArgumentNullException;
import dtu.exceptions.InvalidTokenException;
import dtu.exceptions.NegativeAmountException;

import java.math.BigDecimal;
import java.util.Objects;

public class Payment {

    private final Token token;
    private final BigDecimal amount;
    private final String debtor;
    private final String creditor;
    private final String description;
    private boolean status = false;

    private Payment(PaymentBuilder builder) {
        this.token = builder.token;
        this.amount = builder.amount;
        this.creditor = builder.creditor;
        this.debtor = builder.debtor;
        this.description = builder.description;
    }

    public String getDebtor() {
        return debtor;
    }
    public String getCreditor() {
        return creditor;
    }
    public Token getToken() {
        return token;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public boolean isStatus() {
        return status;
    }
    public void updateStatus(boolean status) {this.status = status;}

    public String getDescription() { return description; }

    public static class PaymentBuilder {

        private String debtor;
        private String creditor;
        private BigDecimal amount;
        private Token token;
        private String description;

        public PaymentBuilder debtor(String debtor) throws ArgumentNullException {
            if (debtor == null) throw new ArgumentNullException("debtor Id must not be null");
            this.debtor = debtor;
            return this;
        }
        public PaymentBuilder creditor(String creditor) throws ArgumentNullException {
            if (creditor == null) throw new ArgumentNullException("creditor Id must not be null");
            this.creditor = creditor;
            return this;
        }
        public PaymentBuilder description(String description) {
            this.description = description;
            return this;
        }
        public PaymentBuilder amount(String amount) throws ArgumentNullException, AmountIsNotANumberException, NegativeAmountException {
            if (amount == null) throw new ArgumentNullException("Amount must not be null");
            try {
                if (amount.equals("-500") || amount.equals("abc")) {
                    System.out.println();
                }
                var number = new BigDecimal(amount);
                if (number.compareTo(new BigDecimal("0")) < 0) throw new NegativeAmountException("Amount must be a positive number");
            } catch (NumberFormatException e) {
                throw new AmountIsNotANumberException("Amount must be a number");
            }
            this.amount = new BigDecimal(amount);
            return this;
        }
        public PaymentBuilder token(Token token) throws ArgumentNullException, InvalidTokenException {
            if (token == null) throw new ArgumentNullException("Token must not be null");
            if (!token.getValidToken()) throw new InvalidTokenException("Token must be valid");
            this.token = token;
            return this;
        }

        public Payment build() {
            return new Payment(this);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Payment payment = (Payment) o;

        if (status != payment.status) return false;
        if (!Objects.equals(creditor, payment.creditor)) return false;
        if (!Objects.equals(token, payment.token)) return false;
        if (!Objects.equals(amount, payment.amount)) return false;
        return Objects.equals(debtor, payment.debtor);
    }

    @Override
    public int hashCode() {
        int result = creditor != null ? creditor.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (debtor != null ? debtor.hashCode() : 0);
        result = 31 * result + (status ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Payment [amount=" + amount + ", creditor=" + creditor + ", debtor=" + debtor + ", description="
                + description + ", status=" + status + ", token=" + token + "]";
    }

}
