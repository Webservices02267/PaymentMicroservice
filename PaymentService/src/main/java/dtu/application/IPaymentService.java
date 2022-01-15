package dtu.application;

import dtu.domain.Payment;
import dtu.domain.Token;
import dtu.exceptions.*;
import dtu.presentation.PaymentDTO;

import java.util.Collection;

public interface IPaymentService {
    boolean pay(Payment payment) throws DebtorHasNoBankAccountException, CreditorHasNoBankAccountException, InsufficientBalanceException;

    Collection<Payment> getPayments();

    boolean pay(PaymentDTO dto, Token token) throws NegativeAmountException, ArgumentNullException, AmountIsNotANumberException, InvalidTokenException, DebtorHasNoBankAccountException, CreditorHasNoBankAccountException, InsufficientBalanceException;
}
