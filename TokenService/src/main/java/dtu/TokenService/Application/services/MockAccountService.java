package dtu.group2.app.services;

import dtu.group2.app.services.interfaces.AccountService;

import java.util.HashSet;

public class MockAccountService implements AccountService {

    static HashSet<String> customers = new HashSet<>();
    static HashSet<String> merchants = new HashSet<>();

    @Override
    public void registerCustomer(String customerId) {
        customers.add(customerId);
    }

    @Override
    public void registerMerchant(String merchantId) {
        merchants.add(merchantId);
    }

    @Override
    public boolean hasCustomer(String customerId) {
        return customers.contains(customerId);
    }
}
