package dtu.group2.app.services.interfaces;

public interface AccountService {
    void registerCustomer(String customerId);

    void registerMerchant(String merchantId);

    boolean hasCustomer(String customerId);
}
