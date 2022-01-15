package dtu.application;



import java.util.Collection;

import dtu.domain.Payment;
import dtu.domain.Token;
import dtu.exceptions.*;
import dtu.infrastructure.interfaces.IPaymentRepository;
import dtu.presentation.PaymentDTO;
import dtu.ws.fastmoney.BankService;

public class PaymentServiceImplementation implements IPaymentService {

    BankServiceWrapper bankService;
    IPaymentRepository paymentRepository;

    public PaymentServiceImplementation(BankService bankService, IPaymentRepository paymentRepository) {
        this.bankService = new BankServiceWrapper(bankService);
        this.paymentRepository = paymentRepository;
    }

    public PaymentServiceImplementation(BankServiceWrapper bankService, IPaymentRepository paymentRepository) {
        this.bankService = bankService;
        this.paymentRepository = paymentRepository;
    }



    @Override
    public boolean pay(Payment payment) throws DebtorHasNoBankAccountException, CreditorHasNoBankAccountException, InsufficientBalanceException {
        try {
            bankService.transferMoney(payment);
            payment.updateStatus(true);
            paymentRepository.addPayment(payment);
            return true;
        } catch (DebtorHasNoBankAccountException | CreditorHasNoBankAccountException e) {
            payment.updateStatus(false);
            paymentRepository.addPayment(payment);
            throw e;
        }
    }

    public boolean pay(PaymentDTO dto, Token token) throws NegativeAmountException, ArgumentNullException, AmountIsNotANumberException, InvalidTokenException, DebtorHasNoBankAccountException, CreditorHasNoBankAccountException, InsufficientBalanceException {
        return pay(new Payment.PaymentBuilder().amount(dto.amount).creditor(dto.merchant).debtor(token.getCustomerId()).token(token).build());
    }



    @Override
    public Collection<Payment> getPayments() {
        return paymentRepository.getPayments();
    }

}
