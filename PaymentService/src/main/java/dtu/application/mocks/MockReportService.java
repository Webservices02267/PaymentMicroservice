package dtu.application.mocks;


import java.util.Objects;
import java.util.stream.Collectors;

import dtu.application.BankServiceWrapper;
import dtu.application.IPaymentService;
import dtu.application.PaymentServiceImplementation;
import dtu.application.interfaces.IReportService;
import dtu.domain.Report;
import dtu.infrastructure.InMemoryRepository;

public class MockReportService implements IReportService {

    private final IPaymentService paymentService = new PaymentServiceImplementation(new BankServiceWrapper(new MockBankService()), new InMemoryRepository());

    @Override
    public Report getCustomerPayments(String customerId) {
        var payments = paymentService.getPayments()
                .stream().filter(payment -> Objects.equals(payment.getDebtor(), customerId))
                .collect(Collectors.toList());
        return new Report(payments);
    }



}
