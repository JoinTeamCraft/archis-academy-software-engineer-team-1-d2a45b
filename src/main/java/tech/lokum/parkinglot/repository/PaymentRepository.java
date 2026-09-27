package tech.lokum.parkinglot.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentStatus;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByStatus(
            PaymentStatus status);
}