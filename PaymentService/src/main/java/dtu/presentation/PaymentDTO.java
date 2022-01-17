package dtu.presentation;

public class PaymentDTO {
    public String mid;
    public String amount;
    public String token;
    public String description;

    public PaymentDTO() {

    }

    public PaymentDTO(String merchantId, String token, String amount) {
        this.mid = merchantId;
        this.token = token;
        this.amount = amount;
    }

}
