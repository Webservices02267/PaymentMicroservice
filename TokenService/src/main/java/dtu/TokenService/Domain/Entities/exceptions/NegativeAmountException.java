package dtu.TokenService.Domain.Entities.exceptions;

public class NegativeAmountException extends Throwable {
    public NegativeAmountException(String s) {
        super(s);
    }
}
