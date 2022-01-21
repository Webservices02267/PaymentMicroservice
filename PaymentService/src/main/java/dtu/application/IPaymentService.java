package dtu.application;

import dtu.domain.Payment;
import dtu.exceptions.*;

import java.util.Collection;

public interface IPaymentService {
    boolean pay(Payment payment) throws DebtorHasNoBankAccountException, CreditorHasNoBankAccountException, InsufficientBalanceException;
    String getStatus();
    Collection<Payment> getPayments();

}
