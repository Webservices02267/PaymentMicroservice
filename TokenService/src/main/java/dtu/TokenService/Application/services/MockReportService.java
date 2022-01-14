package dtu.TokenService.Application.services;


import java.util.Objects;
import java.util.stream.Collectors;

import dtu.TokenService.Application.repos.LocalPaymentRepository;
import dtu.TokenService.Application.services.interfaces.ReportService;
import dtu.TokenService.Domain.Entities.Report;

public class MockReportService implements ReportService {

    private final PaymentService paymentService = new PaymentService(new LocalPaymentRepository());

    @Override
    public Report getCustomerPayments(String customerId) {
        var payments = paymentService.getPayments()
                .stream().filter(payment -> Objects.equals(payment.getCustomerId(), customerId))
                .collect(Collectors.toList());
        return new Report(payments);
    }



}
