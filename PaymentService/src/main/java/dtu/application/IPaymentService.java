package dtu.application;

import dtu.domain.Payment;
import dtu.domain.Token;
import dtu.exceptions.*;
import dtu.presentation.PaymentDTO;

import java.util.Collection;

public interface IPaymentService {
    boolean pay(Payment payment) throws DebtorHasNoBankAccountException, CreditorHasNoBankAccountException, InsufficientBalanceException;
    String getStatus();
    Collection<Payment> getPayments();

}
