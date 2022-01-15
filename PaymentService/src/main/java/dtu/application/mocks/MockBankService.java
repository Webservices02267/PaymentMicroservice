package dtu.application.mocks;

import dtu.ws.fastmoney.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MockBankService implements BankService {

    static HashMap<String, Account> accountHashMap = new HashMap<>();

    @Override
    public Account getAccount(String accountId) throws BankServiceException_Exception {
        var account = accountHashMap.get(accountId);
        if (account == null) throw new BankServiceException_Exception(null, null, null);
        return account;
    }

    @Override
    public String createAccountWithBalance(User user, BigDecimal balance) throws BankServiceException_Exception {
        String accountId = UUID.randomUUID().toString();
        Account account = new Account();
        account.setBalance(balance);
        account.setUser(user);
        account.setId(accountId);
        accountHashMap.put(accountId, account);
        return accountId;
    }

    @Override
    public void retireAccount(String accountId) throws BankServiceException_Exception {
        accountHashMap.remove(accountId);
    }

    @Override
    public List<AccountInfo> getAccounts() {
        return null;
    }

    @Override
    public void transferMoneyFromTo(String debtor, String creditor, BigDecimal amount, String description) throws BankServiceException_Exception {
        var customer = accountHashMap.get(debtor);
        var merchant = accountHashMap.get(creditor);

        customer.setBalance(customer.getBalance().subtract(amount));
        merchant.setBalance(merchant.getBalance().add(amount));
    }
}
