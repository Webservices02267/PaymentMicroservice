package dtu.Application;

public interface IAccountService {
    void registerCustomer(String customerId);

    void registerMerchant(String merchantId);

    boolean hasCustomer(String customerId);
}
