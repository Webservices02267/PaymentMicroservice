package dtu.presentation;

public class PaymentDTO {
    public String merchant;
    public String amount;
    public String token;
    public String description;

    public PaymentDTO() {

    }

    public PaymentDTO(String merchantId, String token, String amount) {
        this.merchant = merchantId;
        this.token = token;
        this.amount = amount;
    }

}
