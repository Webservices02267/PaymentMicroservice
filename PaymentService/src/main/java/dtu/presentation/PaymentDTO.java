package dtu.presentation;


public class PaymentDTO {

    public String token;
    public String merchant;
    public String amount;
    public String description;

    public PaymentDTO(String merchant, String token, String amount) {
        this.merchant = merchant;
        this.token = token;
        this.amount = amount;
    }

}
