package dtu.TokenService.Application.services.interfaces;

import java.util.ArrayList;

import dtu.TokenService.Domain.Entities.Payment;

public interface IPaymentService {
    boolean pay(Payment payment) throws Exception;

    ArrayList<Payment> getPayments();
}
