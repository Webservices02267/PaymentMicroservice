package dtu.infrastructure;

import java.util.Collection;
import java.util.HashMap;

import dtu.exceptions.EntityNotFoundException;
import dtu.exceptions.AmountIsNotANumberException;
import dtu.exceptions.ArgumentNullException;
import dtu.exceptions.NegativeAmountException;
import dtu.domain.Payment;
import dtu.infrastructure.interfaces.IPaymentRepository;

public class InMemoryRepository implements IPaymentRepository {

    private final static HashMap<String, Payment> payments = new HashMap<>();

    @Override
    public Payment addPayment(Payment entity) {
        payments.put(entity.getToken().getUuid(), entity);
        return entity;
    }

    @Override
    public Payment getPayment(String token) throws ArgumentNullException, EntityNotFoundException {
        if (token == null) throw new ArgumentNullException("Argument token cannot be null");
        var payment = payments.get(token);
        if (payment == null) throw new EntityNotFoundException("Payment could not be linked to token");
        return payment;
    }

    @Override
    public Collection<Payment> getPayments() {
        return payments.values();
    }



}
