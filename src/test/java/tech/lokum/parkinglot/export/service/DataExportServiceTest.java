package tech.lokum.parkinglot.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.SpotStatus;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.entity.Vehicle;
import tech.lokum.parkinglot.entity.VehicleType;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.export.dto.DataExportRequest;
import tech.lokum.parkinglot.export.dto.ExportedFile;
import tech.lokum.parkinglot.repository.ParkingLotRepository;
import tech.lokum.parkinglot.repository.PaymentRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ParkingLotRepository parkingLotRepository;

    private DataExportServiceImpl dataExportService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        dataExportService = new DataExportServiceImpl(
                reservationRepository,
                userRepository,
                paymentRepository,
                parkingLotRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Export bookings as CSV should include CSV header and reservation row")
    void exportBookings_csv_shouldGenerateValidCsv() {
        User user = new User("johndoe", "john@example.com", "secret123", "John Doe", "555-1234", Role.USER);
        user.setId(10L);

        ParkingLot lot = new ParkingLot("Central Garage", "Downtown", 100, new BigDecimal("5.00"));
        lot.setId(1L);

        ParkingSpot spot = new ParkingSpot("A-101", 1, VehicleType.CAR, SpotStatus.OCCUPIED, new BigDecimal("5.00"), lot);
        spot.setId(20L);

        Vehicle vehicle = new Vehicle("34ABC123", VehicleType.CAR, "Toyota", "Corolla", "Blue", user);
        vehicle.setId(30L);

        Reservation reservation = new Reservation(
                user,
                vehicle,
                spot,
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                new BigDecimal("10.00"),
                ReservationStatus.CONFIRMED
        );
        reservation.setId(100L);

        when(reservationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reservation)));

        DataExportRequest request = new DataExportRequest("bookings", "CSV");
        ExportedFile result = dataExportService.exportData(request);

        assertNotNull(result);
        assertTrue(result.getFileName().startsWith("bookings_export_"));
        assertTrue(result.getFileName().endsWith(".csv"));
        assertEquals("text/csv; charset=UTF-8", result.getContentType());
        assertEquals(1, result.getRecordCount());

        String content = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("id,user_id,user_email,user_name"));
        assertTrue(content.contains("100,10,john@example.com,John Doe,34ABC123,CAR,Central Garage,A-101"));
        assertTrue(content.contains("CONFIRMED"));
    }

    @Test
    @DisplayName("Export bookings as JSON should return valid JSON array")
    void exportBookings_json_shouldGenerateValidJsonArray() {
        User user = new User("johndoe", "john@example.com", "secret123", "John Doe", "555-1234", Role.USER);
        user.setId(10L);

        ParkingLot lot = new ParkingLot("Central Garage", "Downtown", 100, new BigDecimal("5.00"));
        lot.setId(1L);

        ParkingSpot spot = new ParkingSpot("A-101", 1, VehicleType.CAR, SpotStatus.OCCUPIED, new BigDecimal("5.00"), lot);
        spot.setId(20L);

        Vehicle vehicle = new Vehicle("34ABC123", VehicleType.CAR, "Toyota", "Corolla", "Blue", user);
        vehicle.setId(30L);

        Reservation reservation = new Reservation(
                user,
                vehicle,
                spot,
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                new BigDecimal("10.00"),
                ReservationStatus.CONFIRMED
        );
        reservation.setId(100L);

        when(reservationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reservation)));

        DataExportRequest request = new DataExportRequest("bookings", "JSON");
        ExportedFile result = dataExportService.exportData(request);

        assertNotNull(result);
        assertTrue(result.getFileName().endsWith(".json"));
        assertEquals("application/json; charset=UTF-8", result.getContentType());
        assertEquals(1, result.getRecordCount());

        String content = new String(result.getContent(), StandardCharsets.UTF_8).trim();
        assertTrue(content.startsWith("["));
        assertTrue(content.endsWith("]"));
        assertTrue(content.contains("\"id\":100"));
        assertTrue(content.contains("\"userEmail\":\"john@example.com\""));
    }

    @Test
    @DisplayName("Export users should never include passwords in CSV or JSON")
    void exportUsers_shouldOmitPasswords() {
        User user = new User("secureuser", "secure@example.com", "$2a$10$hashedsecretpassword", "Alice Smith", "555-5678", Role.MANAGER);
        user.setId(1L);

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        DataExportRequest request = new DataExportRequest("users", "CSV");
        ExportedFile result = dataExportService.exportData(request);

        String content = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("secureuser"));
        assertTrue(content.contains("secure@example.com"));
        assertTrue(content.contains("Alice Smith"));
        assertFalse(content.contains("hashedsecretpassword"), "Password hashes must never be exposed in exports");
    }

    @Test
    @DisplayName("Export payments as CSV should include transaction details")
    void exportPayments_csv_shouldGeneratePaymentRecords() {
        Payment payment = new Payment(
                null,
                new BigDecimal("25.50"),
                PaymentMethod.CREDIT_CARD,
                PaymentStatus.SUCCESS,
                "txn_stripe_9999",
                Instant.parse("2026-10-01T12:00:00Z"),
                "USD"
        );
        payment.setId(50L);

        when(paymentRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));

        DataExportRequest request = new DataExportRequest("payments", "CSV");
        ExportedFile result = dataExportService.exportData(request);

        String content = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("id,reservation_id,amount,currency,payment_method,status,transaction_id"));
        assertTrue(content.contains("50,,25.50,USD,CREDIT_CARD,SUCCESS,txn_stripe_9999"));
    }

    @Test
    @DisplayName("Export parking lots as CSV should list parking lot facilities")
    void exportParkingLots_csv_shouldGenerateLotRecords() {
        ParkingLot lot = new ParkingLot("North Port Lot", "Airport Road", 250, new BigDecimal("8.00"));
        lot.setId(5L);

        when(parkingLotRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lot)));

        DataExportRequest request = new DataExportRequest("parking_lots", "CSV");
        ExportedFile result = dataExportService.exportData(request);

        String content = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("5,North Port Lot,Airport Road,250,8.00"));
    }

    @Test
    @DisplayName("Unsupported data type should throw BadRequestException")
    void exportData_unsupportedType_shouldThrowBadRequestException() {
        DataExportRequest request = new DataExportRequest("unknown_dataset", "CSV");
        assertThrows(BadRequestException.class, () -> dataExportService.exportData(request));
    }

    @Test
    @DisplayName("Unsupported format should throw BadRequestException")
    void exportData_unsupportedFormat_shouldThrowBadRequestException() {
        DataExportRequest request = new DataExportRequest("bookings", "XML");
        assertThrows(BadRequestException.class, () -> dataExportService.exportData(request));
    }
}
