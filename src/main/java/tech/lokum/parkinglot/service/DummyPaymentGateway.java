package tech.lokum.parkinglot.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentStatus;

@Service
public class DummyPaymentGateway implements PaymentGateway {

    @Override
    public Payment process(Payment payment) {

        payment.setStatus(PaymentStatus.SUCCESS);

        payment.setTransactionReference(
                "TXN-" + UUID.randomUUID());

        payment.setPaidAt(LocalDateTime.now());

        return payment;
    }
}
