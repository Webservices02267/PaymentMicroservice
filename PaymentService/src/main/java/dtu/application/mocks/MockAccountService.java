package dtu.application.mocks;

import dtu.application.interfaces.IAccountService;

import java.util.HashSet;

public class MockAccountService implements IAccountService {

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

    @Override
    public boolean hasMerchant(String merchantId) {
        return merchants.contains(merchantId);
    }
}
