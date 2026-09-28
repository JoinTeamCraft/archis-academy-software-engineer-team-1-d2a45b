package tech.lokum.parkinglot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.entity.Vehicle;
import tech.lokum.parkinglot.entity.VehicleType;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        User user = new User("customer@example.com", "pass", "Customer One", "555-0100", Role.CUSTOMER);
        user.setId(10L);

        Vehicle vehicle = new Vehicle("PAY-100", VehicleType.CAR, "Honda", "Civic", "White", user);
        vehicle.setId(20L);

        ParkingLot lot = new ParkingLot("Main Lot", "123 Street", 50);
        lot.setId(1L);

        ParkingSpot spot = new ParkingSpot("S-01", 1, VehicleType.CAR, lot);
        spot.setId(101L);

        reservation = new Reservation(user, vehicle, spot, Instant.now(), Instant.now().plusSeconds(7200), BigDecimal.valueOf(25.00), ReservationStatus.CONFIRMED);
        reservation.setId(501L);

        payment = new Payment(
            reservation,
            BigDecimal.valueOf(25.00),
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.SUCCESS,
            "STRIPE123456",
            Instant.now(),
            "USD"
        );
        payment.setId(301L);
    }

    @Test
    @DisplayName("getPaymentById should return PaymentResponse with all expected details when found")
    void shouldReturnPaymentResponseWhenPaymentExists() {
        when(paymentRepository.findByIdWithReservation(301L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById(301L);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(301L);
        assertThat(response.reservationId()).isEqualTo(501L);
        assertThat(response.amount()).isEqualByComparingTo("25.00");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.confirmationNumber()).isEqualTo("STRIPE123456");

        verify(paymentRepository).findByIdWithReservation(301L);
    }

    @Test
    @DisplayName("getPaymentById should fall back to findById when findByIdWithReservation returns empty")
    void shouldFallbackToFindByIdWhenReservationQueryEmpty() {
        when(paymentRepository.findByIdWithReservation(301L)).thenReturn(Optional.empty());
        when(paymentRepository.findById(301L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById(301L);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(301L);
        assertThat(response.reservationId()).isEqualTo(501L);
    }

    @Test
    @DisplayName("getPaymentById should throw ResourceNotFoundException when payment ID does not exist")
    void shouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(paymentRepository.findByIdWithReservation(999L)).thenReturn(Optional.empty());
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Payment not found with id: '999'");
    }

    @Test
    @DisplayName("getPaymentById should throw BadRequestException when payment ID is null")
    void shouldThrowBadRequestExceptionWhenPaymentIdIsNull() {
        assertThatThrownBy(() -> paymentService.getPaymentById(null))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Payment ID must be a positive number");
    }

    @Test
    @DisplayName("getPaymentById should throw BadRequestException when payment ID is non-positive")
    void shouldThrowBadRequestExceptionWhenPaymentIdIsZeroOrNegative() {
        assertThatThrownBy(() -> paymentService.getPaymentById(0L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Payment ID must be a positive number");

        assertThatThrownBy(() -> paymentService.getPaymentById(-10L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Payment ID must be a positive number");
    }

    @Test
    @DisplayName("getAllPayments without filters should return all payments")
    void shouldReturnAllPaymentsWithoutFilters() {
        when(paymentRepository.findPaymentsWithFilters(null, null)).thenReturn(java.util.List.of(payment));

        java.util.List<PaymentResponse> results = paymentService.getAllPayments(null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).paymentId()).isEqualTo(301L);
        verify(paymentRepository).findPaymentsWithFilters(null, null);
    }

    @Test
    @DisplayName("getAllPayments with status filter should delegate to repository")
    void shouldFilterPaymentsByStatus() {
        when(paymentRepository.findPaymentsWithFilters(PaymentStatus.SUCCESS, null)).thenReturn(java.util.List.of(payment));

        java.util.List<PaymentResponse> results = paymentService.getAllPayments(PaymentStatus.SUCCESS, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).findPaymentsWithFilters(PaymentStatus.SUCCESS, null);
    }

    @Test
    @DisplayName("getAllPayments with reservationId filter should delegate to repository")
    void shouldFilterPaymentsByReservationId() {
        when(paymentRepository.findPaymentsWithFilters(null, 501L)).thenReturn(java.util.List.of(payment));

        java.util.List<PaymentResponse> results = paymentService.getAllPayments(null, 501L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).reservationId()).isEqualTo(501L);
        verify(paymentRepository).findPaymentsWithFilters(null, 501L);
    }

    @Test
    @DisplayName("getAllPayments with both status and reservationId filters should delegate to repository")
    void shouldFilterPaymentsByBothStatusAndReservationId() {
        when(paymentRepository.findPaymentsWithFilters(PaymentStatus.SUCCESS, 501L)).thenReturn(java.util.List.of(payment));

        java.util.List<PaymentResponse> results = paymentService.getAllPayments(PaymentStatus.SUCCESS, 501L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).paymentId()).isEqualTo(301L);
        verify(paymentRepository).findPaymentsWithFilters(PaymentStatus.SUCCESS, 501L);
    }

    @Test
    @DisplayName("getAllPayments with pagination should return paginated response")
    void shouldReturnPaginatedPayments() {
        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAll(pageable)).thenReturn(new PageImpl<>(java.util.List.of(payment), pageable, 1));

        Page<PaymentResponse> page = paymentService.getAllPayments(null, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).paymentId()).isEqualTo(301L);
    }
}
