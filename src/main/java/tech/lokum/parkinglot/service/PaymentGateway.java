package tech.lokum.parkinglot.service;

import tech.lokum.parkinglot.entity.Payment;

public interface PaymentGateway {

    Payment process(Payment payment);
}
