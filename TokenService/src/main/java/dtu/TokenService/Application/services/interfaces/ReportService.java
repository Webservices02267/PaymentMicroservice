package dtu.TokenService.Application.services.interfaces;

import dtu.TokenService.Domain.Entities.Report;

public interface ReportService {
    Report getCustomerPayments(String customerId);
}
