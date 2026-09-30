package tech.lokum.parkinglot.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.report.dto.ReportRequest;
import tech.lokum.parkinglot.report.dto.ReportResponse;
import tech.lokum.parkinglot.report.entity.ReportMetadata;
import tech.lokum.parkinglot.report.exporter.CsvReportExporter;
import tech.lokum.parkinglot.report.exporter.PdfReportExporter;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.GeneratedReport;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.ReportType;
import tech.lokum.parkinglot.report.model.UserActivityReportData;
import tech.lokum.parkinglot.report.repository.ReportMetadataRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReportMetadataRepository reportMetadataRepository;

    private CsvReportExporter csvExporter;
    private PdfReportExporter pdfExporter;
    private ReportServiceImpl reportService;

    private User alice;
    private User bob;
    private ParkingLot lot;
    private ParkingSpot spot;
    private Vehicle vehicle;
    private Reservation reservation;
    private Payment payment;

    @BeforeEach
    void setUp() {
        csvExporter = new CsvReportExporter();
        pdfExporter = new PdfReportExporter();
        reportService = new ReportServiceImpl(
                userRepository,
                reservationRepository,
                paymentRepository,
                reportMetadataRepository,
                List.of(csvExporter, pdfExporter)
        );

        alice = new User("alice@example.com", "pass", "Alice Smith", "+123456", Role.CUSTOMER);
        alice.setId(1L);
        alice.setUsername("alice_s");
        alice.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));

        bob = new User("bob@example.com", "pass", "Bob Jones", "+654321", Role.ADMIN);
        bob.setId(2L);
        bob.setUsername("bob_admin");
        bob.setActive(false);
        bob.setCreatedAt(Instant.now().minus(5, ChronoUnit.DAYS));

        lot = new ParkingLot("Central Garage", "123 Center St", 150);
        lot.setId(10L);

        spot = new ParkingSpot("A-01", 1, VehicleType.CAR, lot);
        spot.setId(20L);

        vehicle = new Vehicle("XYZ-789", VehicleType.CAR, "Honda", "Civic", "Black", alice);
        vehicle.setId(30L);

        Instant start = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant end = start.plus(3, ChronoUnit.HOURS);
        reservation = new Reservation(alice, vehicle, spot, start, end, BigDecimal.valueOf(45.00), ReservationStatus.CONFIRMED);
        reservation.setId(100L);
        reservation.setCreatedAt(start.minus(1, ChronoUnit.HOURS));

        payment = new Payment(reservation, BigDecimal.valueOf(45.00), PaymentMethod.CREDIT_CARD, PaymentStatus.SUCCESS, "TX-123456", start);
        payment.setId(200L);
        payment.setCreatedAt(start);
    }

    @Test
    @DisplayName("fetchUserActivityData should aggregate user counts, roles, and spend")
    void shouldFetchUserActivityData() {
        when(userRepository.findUsersForReport(any(), any())).thenReturn(List.of(alice, bob));
        when(reservationRepository.findReservationsForReport(any(), any())).thenReturn(List.of(reservation));

        UserActivityReportData data = reportService.fetchUserActivityData(null, null);

        assertThat(data.getTotalUsers()).isEqualTo(2);
        assertThat(data.getActiveUsers()).isEqualTo(1);
        assertThat(data.getInactiveUsers()).isEqualTo(1);
        assertThat(data.getUsersByRole()).containsEntry("CUSTOMER", 1L).containsEntry("ADMIN", 1L);
        assertThat(data.getItems()).hasSize(2);

        UserActivityReportData.UserActivityItem aliceItem = data.getItems().stream()
                .filter(i -> i.getUserId().equals(1L))
                .findFirst().orElseThrow();
        assertThat(aliceItem.getTotalReservations()).isEqualTo(1);
        assertThat(aliceItem.getTotalSpent()).isEqualByComparingTo(BigDecimal.valueOf(45.00));
        assertThat(aliceItem.isActive()).isTrue();
    }

    @Test
    @DisplayName("fetchBookingTrendsData should aggregate booking statuses, lots, and durations")
    void shouldFetchBookingTrendsData() {
        when(reservationRepository.findReservationsForReport(any(), any())).thenReturn(List.of(reservation));

        BookingTrendsReportData data = reportService.fetchBookingTrendsData(null, null);

        assertThat(data.getTotalBookings()).isEqualTo(1);
        assertThat(data.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(45.00));
        assertThat(data.getAverageDurationHours()).isEqualTo(3.0);
        assertThat(data.getBookingsByStatus()).containsEntry("CONFIRMED", 1L);
        assertThat(data.getBookingsByLot()).containsEntry("Central Garage", 1L);
        assertThat(data.getItems()).hasSize(1);
        assertThat(data.getItems().get(0).getCustomerName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("fetchPaymentSummaryData should aggregate revenue, payment methods, and status")
    void shouldFetchPaymentSummaryData() {
        when(paymentRepository.findPaymentsForReport(any(), any())).thenReturn(List.of(payment));

        PaymentSummaryReportData data = reportService.fetchPaymentSummaryData(null, null);

        assertThat(data.getTotalTransactions()).isEqualTo(1);
        assertThat(data.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(45.00));
        assertThat(data.getPaymentsByStatus()).containsEntry("SUCCESS", 1L);
        assertThat(data.getRevenueByMethod()).containsEntry("CREDIT_CARD", BigDecimal.valueOf(45.00));
        assertThat(data.getItems()).hasSize(1);
        assertThat(data.getItems().get(0).getTransactionId()).isEqualTo("TX-123456");
    }

    @Test
    @DisplayName("generateReport should produce valid CSV and PDF files for all report types")
    void shouldGenerateReportsInBothFormats() {
        when(userRepository.findUsersForReport(any(), any())).thenReturn(List.of(alice));
        when(reservationRepository.findReservationsForReport(any(), any())).thenReturn(List.of(reservation));
        when(paymentRepository.findPaymentsForReport(any(), any())).thenReturn(List.of(payment));

        // User Activity - CSV
        GeneratedReport userCsv = reportService.generateReport(ReportType.USER_ACTIVITY, ReportFormat.CSV, null, null);
        assertThat(userCsv.getContentType()).isEqualTo("text/csv; charset=UTF-8");
        assertThat(userCsv.getFileName()).endsWith(".csv");
        assertThat(new String(userCsv.getContent())).contains("USER ACTIVITY REPORT", "Alice Smith", "alice@example.com");

        // User Activity - PDF
        GeneratedReport userPdf = reportService.generateReport(ReportType.USER_ACTIVITY, ReportFormat.PDF, null, null);
        assertThat(userPdf.getContentType()).isEqualTo("application/pdf");
        assertThat(userPdf.getFileName()).endsWith(".pdf");
        assertThat(userPdf.getContent().length).isGreaterThan(100);

        // Booking Trends - CSV
        GeneratedReport bookingCsv = reportService.generateReport(ReportType.BOOKING_TRENDS, ReportFormat.CSV, null, null);
        assertThat(new String(bookingCsv.getContent())).contains("BOOKING TRENDS REPORT", "Central Garage", "$45.00");

        // Booking Trends - PDF
        GeneratedReport bookingPdf = reportService.generateReport(ReportType.BOOKING_TRENDS, ReportFormat.PDF, null, null);
        assertThat(bookingPdf.getContent().length).isGreaterThan(100);

        // Payment Summary - CSV
        GeneratedReport paymentCsv = reportService.generateReport(ReportType.PAYMENT_SUMMARY, ReportFormat.CSV, null, null);
        assertThat(new String(paymentCsv.getContent())).contains("PAYMENT SUMMARY REPORT", "TX-123456");

        // Payment Summary - PDF
        GeneratedReport paymentPdf = reportService.generateReport(ReportType.PAYMENT_SUMMARY, ReportFormat.PDF, null, null);
        assertThat(paymentPdf.getContent().length).isGreaterThan(100);
    }

    @Test
    @DisplayName("requestReport should generate report, persist ReportMetadata, and return ReportResponse")
    void shouldRequestReportSuccessfully() {
        when(paymentRepository.findPaymentsForReport(any(), any())).thenReturn(List.of(payment));
        when(reportMetadataRepository.save(any(ReportMetadata.class))).thenAnswer(inv -> {
            ReportMetadata rm = inv.getArgument(0);
            rm.setId(123L);
            return rm;
        });

        ReportRequest request = new ReportRequest("monthlyRevenue", "2025-01-01", "2025-01-31", "CSV");
        ReportResponse response = reportService.requestReport(request, "admin@example.com");

        assertThat(response.getReportId()).isEqualTo(123L);
        assertThat(response.getStatus()).isEqualTo("generated");
        assertThat(response.getDownloadLink()).isEqualTo("/api/reports/download/123");
    }

    @Test
    @DisplayName("requestReport should throw BadRequestException when reportType is missing or invalid")
    void shouldThrowBadRequestForInvalidReportType() {
        assertThatThrownBy(() -> reportService.requestReport(new ReportRequest("", "2025-01-01", "2025-01-31"), "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reportType is required");

        assertThatThrownBy(() -> reportService.requestReport(new ReportRequest("unknownType", "2025-01-01", "2025-01-31"), "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported reportType");
    }

    @Test
    @DisplayName("getReportMetadata should return metadata or throw ResourceNotFoundException")
    void shouldGetReportMetadata() {
        ReportMetadata metadata = new ReportMetadata("monthlyRevenue", "CSV", "generated", "2025-01-01", "2025-01-31", "test.csv", "text/csv", "/api/reports/download/99", "admin", "content".getBytes());
        metadata.setId(99L);

        when(reportMetadataRepository.findById(99L)).thenReturn(Optional.of(metadata));
        when(reportMetadataRepository.findById(999L)).thenReturn(Optional.empty());

        ReportMetadata found = reportService.getReportMetadata(99L);
        assertThat(found.getId()).isEqualTo(99L);
        assertThat(found.getFileName()).isEqualTo("test.csv");

        assertThatThrownBy(() -> reportService.getReportMetadata(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found with id: 999");
    }

    @Test
    @DisplayName("generateReport should throw IllegalArgumentException when format is missing an exporter")
    void shouldThrowWhenNoExporter() {
        ReportServiceImpl emptyService = new ReportServiceImpl(userRepository, reservationRepository, paymentRepository, reportMetadataRepository, List.of());

        assertThatThrownBy(() -> emptyService.generateUserActivityReport(null, null, ReportFormat.PDF))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No exporter registered for format: PDF");
    }
}
