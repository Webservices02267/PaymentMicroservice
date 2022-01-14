package dtu.Application;

import dtu.Domain.Report;

public interface IReportService {
    Report getCustomerPayments(String customerId);
}
