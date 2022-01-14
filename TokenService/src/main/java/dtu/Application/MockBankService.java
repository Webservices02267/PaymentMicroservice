package dtu.Application;

import dtu.ws.fastmoney.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MockBankService implements BankService {

    static HashMap<String, Account> accountHashMap = new HashMap<>();

    @Override
    public Account getAccount(String accountId) throws BankServiceException_Exception {
        return accountHashMap.get(accountId);
    }

    @Override
    public String createAccountWithBalance(User user, BigDecimal balance) throws BankServiceException_Exception {
        String accountId = UUID.randomUUID().toString();
        Account account = new Account();
        account.setBalance(balance);
        account.setUser(user);
        account.setId(accountId);
        accountHashMap.put(accountId, account);
    /*
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }

     */

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
