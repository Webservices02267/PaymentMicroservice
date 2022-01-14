package dtu.TokenService.Domain.Entities.exceptions;

public class AmountIsNotANumberException extends Throwable {
    public AmountIsNotANumberException(String s) {
        super(s);
    }
}
