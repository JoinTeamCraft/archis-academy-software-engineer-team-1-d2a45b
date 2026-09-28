package tech.lokum.parkinglot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a financial payment made for a parking reservation.
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_reservation", columnList = "reservation_id"),
        @Index(name = "idx_payment_tx_id", columnList = "transaction_id", unique = true)
    }
)
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "paid_at")
    private Instant paidAt;

    public Payment() {
    }

    public Payment(Reservation reservation, BigDecimal amount, PaymentMethod paymentMethod) {
        this(reservation, amount, paymentMethod, PaymentStatus.PENDING, null, null, "USD");
    }

    public Payment(Reservation reservation, BigDecimal amount, PaymentMethod paymentMethod, PaymentStatus status, String transactionId, Instant paidAt) {
        this(reservation, amount, paymentMethod, status, transactionId, paidAt, "USD");
    }

    public Payment(Reservation reservation, BigDecimal amount, PaymentMethod paymentMethod, PaymentStatus status, String transactionId, Instant paidAt, String currency) {
        this.reservation = reservation;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.transactionId = transactionId;
        this.paidAt = paidAt;
        this.currency = currency != null ? currency : "USD";
    }

    public void markSuccess(String transactionId) {
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.paidAt = Instant.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getConfirmationNumber() {
        return transactionId;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.transactionId = confirmationNumber;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }
}
