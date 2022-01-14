package dtu.Application;



import java.util.Collection;

import dtu.Domain.Payment;

public interface IPaymentRepository {
    public Collection<Payment> getAll();
    public Payment create(Payment payment);



}
