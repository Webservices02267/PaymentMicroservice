package dtu.Application;


import java.util.Objects;
import java.util.stream.Collectors;

import dtu.Domain.Report;

public class MockReportService implements IReportService {

    private final PaymentServiceImplementation paymentService = new PaymentServiceImplementation(new LocalPaymentRepository());

    @Override
    public Report getCustomerPayments(String customerId) {
        var payments = paymentService.getPayments()
                .stream().filter(payment -> Objects.equals(payment.getCustomerId(), customerId))
                .collect(Collectors.toList());
        return new Report(payments);
    }



}
