package dtu.application;

import dtu.domain.Payment;
import dtu.exceptions.DebtorHasNoBankAccountException;
import dtu.exceptions.CreditorHasNoBankAccountException;
import dtu.exceptions.InsufficientBalanceException;
import dtu.ws.fastmoney.*;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.math.BigDecimal;
import java.util.List;

public class BankServiceWrapper {
    private final BankService soapBank;
    public BankServiceWrapper(BankService bankService) {
        this.soapBank = bankService;
    }



    @ResponseWrapper(localName = "getAccountResponse", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.GetAccountResponse")
    @RequestWrapper(localName = "getAccount", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.GetAccount")
    @WebResult(targetNamespace = "")
    @WebMethod
    public Account getAccount(String accountId) throws BankServiceException_Exception {
        return soapBank.getAccount(accountId);
    }

    @ResponseWrapper(localName = "createAccountWithBalanceResponse", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.CreateAccountWithBalanceResponse")
    @RequestWrapper(localName = "createAccountWithBalance", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.CreateAccountWithBalance")
    @WebResult(targetNamespace = "")
    @WebMethod
    public String createAccountWithBalance(User user, BigDecimal balance) throws BankServiceException_Exception {
        return soapBank.createAccountWithBalance(user, balance);
    }

    @ResponseWrapper(localName = "retireAccountResponse", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.RetireAccountResponse")
    @RequestWrapper(localName = "retireAccount", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.RetireAccount")
    @WebMethod
    public void retireAccount(String accountId) throws BankServiceException_Exception {
        soapBank.retireAccount(accountId);
    }

    @ResponseWrapper(localName = "getAccountsResponse", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.GetAccountsResponse")
    @RequestWrapper(localName = "getAccounts", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.GetAccounts")
    @WebResult(targetNamespace = "")
    @WebMethod
    public List<AccountInfo> getAccounts() {
        return soapBank.getAccounts();
    }

    @ResponseWrapper(localName = "transferMoneyFromToResponse", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.TransferMoneyFromToResponse")
    @RequestWrapper(localName = "transferMoneyFromTo", targetNamespace = "http://fastmoney.ws.dtu/", className = "dtu.ws.fastmoney.TransferMoneyFromTo")
    @WebMethod
    public void transferMoney(Payment payment) throws DebtorHasNoBankAccountException, CreditorHasNoBankAccountException, InsufficientBalanceException {
        Account debtor = null;
        try {
            debtor = soapBank.getAccount(payment.getDebtor());
        } catch (BankServiceException_Exception e) {
            throw new DebtorHasNoBankAccountException();
        }

        try {
            soapBank.getAccount(payment.getCreditor());
        } catch (BankServiceException_Exception e) {
            throw new CreditorHasNoBankAccountException();
        }

        if (debtor.getBalance().compareTo(payment.getAmount()) < 0) throw new InsufficientBalanceException("Insufficient balance on debtor account");

        try {
            soapBank.transferMoneyFromTo(payment.getDebtor(), payment.getCreditor(), payment.getAmount(), payment.getDescription());
        } catch (BankServiceException_Exception ignored) {
        }
    }
}
