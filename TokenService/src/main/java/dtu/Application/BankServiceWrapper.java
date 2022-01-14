package dtu.Application;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceException_Exception;

import java.math.BigDecimal;

public class BankServiceWrapper {
    public final BankService bs;
    public BankServiceWrapper(BankService bankService) {
        this.bs = bankService;
    }

    public boolean transferMoney(String debtor, String creditor, BigDecimal amount, String description) throws Exception {
        try {
            var customer = bs.getAccount(debtor);
            var merchant = bs.getAccount(creditor);
            if (customer.getBalance().compareTo(amount) < 0) throw new Exception("Insufficient Balance on customer account");
            if (amount.compareTo(new BigDecimal("0")) < 0) throw new Exception("Negative amount");
            bs.transferMoneyFromTo(debtor, creditor, amount, description);
            return true;
        } catch (BankServiceException_Exception e) {
            return false;
        }
    }

}
