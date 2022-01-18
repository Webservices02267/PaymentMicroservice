package dtu.presentation;

public class PaymentDTO {
    public String merchantId;
    public String amount;
    public String token;
    public String description;
    public String sessionId;

    public PaymentDTO() {

    }

    public PaymentDTO(String merchantId, String token, String amount) {
        this.merchantId = merchantId;
        this.token = token;
        this.amount = amount;
    }

}
