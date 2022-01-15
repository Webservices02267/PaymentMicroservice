package dtu.infrastructure.interfaces;



import java.util.Collection;

import dtu.exceptions.EntityNotFoundException;
import dtu.exceptions.AmountIsNotANumberException;
import dtu.exceptions.ArgumentNullException;
import dtu.exceptions.NegativeAmountException;
import dtu.domain.Payment;

public interface IPaymentRepository {
    Payment addPayment(Payment payment);
    Payment getPayment(String token) throws ArgumentNullException, EntityNotFoundException;
    Collection<Payment> getPayments();




}
