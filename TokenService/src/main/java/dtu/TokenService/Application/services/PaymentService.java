package dtu.TokenService.Application.services;



import dtu.TokenService.Application.TokenService;
import dtu.TokenService.Domain.Entities.Payment;
import dtu.ws.fastmoney.BankService;


import java.math.BigDecimal;
import java.util.ArrayList;

public class PaymentService implements dtu.TokenService.Application.services.interfaces.PaymentService {

    MockTokenService tokenService = new MockTokenService();
    BankServiceWrapper bankService = new BankServiceWrapper(new MockBankService());
    dtu.TokenService.Application.repos.PaymentRepository paymentRepository;


    public PaymentService(dtu.TokenService.Application.repos.PaymentRepository paymentRepository) {
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
