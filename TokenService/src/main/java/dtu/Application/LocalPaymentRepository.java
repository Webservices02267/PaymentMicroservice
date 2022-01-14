package dtu.Application;


import java.util.ArrayList;

import dtu.Domain.Payment;

public class LocalPaymentRepository implements IPaymentRepository {


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
