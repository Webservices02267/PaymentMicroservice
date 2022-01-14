package dtu.TokenService.Application.repos;


import java.util.ArrayList;

import dtu.TokenService.Domain.Entities.Payment;

public class LocalPaymentRepository implements PaymentRepository {


    private final static ArrayList<Payment> payments = new ArrayList<>();
    private static int counter = 0;



    @Override
    public ArrayList<Payment> getAll() {
        return payments;
    }

    @Override
    public Payment create(Payment entity) {
        payments.add(entity);
        return entity;
    }

}
