package dtu.Application;



import java.math.BigDecimal;
import java.util.ArrayList;

import dtu.Domain.Payment;

public class PaymentServiceImplementation implements dtu.Application.IPaymentService {

    ITokenService tokenService = new MockTokenService();
    BankServiceWrapper bankService = new BankServiceWrapper(new MockBankService());
    dtu.Application.IPaymentRepository paymentRepository;


    public PaymentServiceImplementation(dtu.Application.IPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public boolean pay(Payment payment) throws Exception {

        var customer = bankService.bs.getAccount(payment.getCustomerId());
        var merchant = bankService.bs.getAccount(payment.getMerchantId());
        paymentRepository.create(payment);
        tokenService.removeToken(payment.getCustomerId(), payment.getToken());
        bankService.transferMoney(payment.getCustomerId(), payment.getMerchantId(), new BigDecimal(payment.getAmount()), "robin hood");


            //TODO: Make one for a merchant in a refund scenario
            //TODO: Unkown token, merchantId and customerId
            //TODO: invalid amounts

        /*
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }

         */
        return true;
    }

    @Override
    public ArrayList<Payment> getPayments() {
        return new ArrayList<>(paymentRepository.getAll());
    }
}
