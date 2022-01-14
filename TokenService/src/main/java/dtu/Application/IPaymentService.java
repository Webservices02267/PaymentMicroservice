package dtu.Application;

import java.util.ArrayList;

import dtu.Domain.Payment;

public interface IPaymentService {
    boolean pay(Payment payment) throws Exception;

    ArrayList<Payment> getPayments();
}
