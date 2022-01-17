package dtu.application.interfaces;

public interface IAccountService {
    void registerCustomer(String customerId);

    void registerMerchant(String merchantId);

    boolean hasCustomer(String customerId);
    boolean hasMerchant(String merchantId);
}
