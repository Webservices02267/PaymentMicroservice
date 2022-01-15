package dtu.application.interfaces;

import dtu.domain.Report;

public interface IReportService {
    Report getCustomerPayments(String customerId);
}
