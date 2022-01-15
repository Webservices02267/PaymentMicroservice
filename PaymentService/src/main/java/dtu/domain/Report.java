package dtu.domain;

import java.util.List;

public class Report {
    private List<Payment> payments;
    public Report(List<Payment> payments) {
        this.payments = payments;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }
}