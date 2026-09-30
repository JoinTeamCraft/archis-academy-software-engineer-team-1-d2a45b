package tech.lokum.parkinglot.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.PaymentRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.report.dto.ReportRequest;
import tech.lokum.parkinglot.report.dto.ReportResponse;
import tech.lokum.parkinglot.report.entity.ReportMetadata;
import tech.lokum.parkinglot.report.exporter.ReportExporter;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.GeneratedReport;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.ReportType;
import tech.lokum.parkinglot.report.model.UserActivityReportData;
import tech.lokum.parkinglot.report.repository.ReportMetadataRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Service implementation for fetching, aggregating, exporting system reports,
 * and persisting report metadata records.
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC);

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ReportMetadataRepository reportMetadataRepository;
    private final Map<ReportFormat, ReportExporter> exporters = new EnumMap<>(ReportFormat.class);

    public ReportServiceImpl(
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            PaymentRepository paymentRepository,
            ReportMetadataRepository reportMetadataRepository,
            List<ReportExporter> exporterList
    ) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.reportMetadataRepository = reportMetadataRepository;
        if (exporterList != null) {
            for (ReportExporter exporter : exporterList) {
                this.exporters.put(exporter.getFormat(), exporter);
            }
        }
    }

    @Override
    @Transactional
    public ReportResponse requestReport(ReportRequest request, String requestedBy) {
        if (request == null || request.getReportType() == null || request.getReportType().isBlank()) {
            throw new BadRequestException("reportType is required");
        }

        ReportType reportType = resolveReportType(request.getReportType());
        ReportFormat reportFormat = resolveReportFormat(request.getFormat());

        Instant start = parseDate(request.getStartDate(), false);
        Instant end = parseDate(request.getEndDate(), true);

        GeneratedReport generated = generateReport(reportType, reportFormat, start, end);

        // Customize file name if specific alias was passed (e.g. monthlyRevenue)
        String fileName = generated.getFileName();
        if ("monthlyrevenue".equalsIgnoreCase(request.getReportType().replaceAll("[_-]", ""))) {
            fileName = String.format("monthly_revenue_report_%s.%s", FILE_DATE_FORMATTER.format(Instant.now()), reportFormat.getExtension());
        }

        ReportMetadata metadata = new ReportMetadata(
                request.getReportType(),
                reportFormat.name(),
                "generated",
                request.getStartDate(),
                request.getEndDate(),
                fileName,
                generated.getContentType(),
                null,
                requestedBy != null ? requestedBy : "system",
                generated.getContent()
        );

        metadata = reportMetadataRepository.save(metadata);

        String downloadLink = "/api/reports/download/" + metadata.getId();
        metadata.setDownloadLink(downloadLink);
        metadata = reportMetadataRepository.save(metadata);

        return new ReportResponse(metadata.getId(), metadata.getStatus(), downloadLink);
    }

    @Override
    public ReportMetadata getReportMetadata(Long reportId) {
        return reportMetadataRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
    }

    @Override
    public GeneratedReport generateReport(ReportType type, ReportFormat format, Instant startDate, Instant endDate) {
        return switch (type) {
            case USER_ACTIVITY -> generateUserActivityReport(startDate, endDate, format);
            case BOOKING_TRENDS -> generateBookingTrendsReport(startDate, endDate, format);
            case PAYMENT_SUMMARY -> generatePaymentSummaryReport(startDate, endDate, format);
        };
    }

    @Override
    public GeneratedReport generateUserActivityReport(Instant startDate, Instant endDate, ReportFormat format) {
        ReportExporter exporter = getExporter(format);
        UserActivityReportData data = fetchUserActivityData(startDate, endDate);
        byte[] bytes = exporter.exportUserActivity(data);

        String fileName = String.format("user_activity_report_%s.%s", FILE_DATE_FORMATTER.format(Instant.now()), format.getExtension());
        return new GeneratedReport(fileName, format.getMediaType(), bytes, ReportType.USER_ACTIVITY, format, Instant.now());
    }

    @Override
    public GeneratedReport generateBookingTrendsReport(Instant startDate, Instant endDate, ReportFormat format) {
        ReportExporter exporter = getExporter(format);
        BookingTrendsReportData data = fetchBookingTrendsData(startDate, endDate);
        byte[] bytes = exporter.exportBookingTrends(data);

        String fileName = String.format("booking_trends_report_%s.%s", FILE_DATE_FORMATTER.format(Instant.now()), format.getExtension());
        return new GeneratedReport(fileName, format.getMediaType(), bytes, ReportType.BOOKING_TRENDS, format, Instant.now());
    }

    @Override
    public GeneratedReport generatePaymentSummaryReport(Instant startDate, Instant endDate, ReportFormat format) {
        ReportExporter exporter = getExporter(format);
        PaymentSummaryReportData data = fetchPaymentSummaryData(startDate, endDate);
        byte[] bytes = exporter.exportPaymentSummary(data);

        String fileName = String.format("payment_summary_report_%s.%s", FILE_DATE_FORMATTER.format(Instant.now()), format.getExtension());
        return new GeneratedReport(fileName, format.getMediaType(), bytes, ReportType.PAYMENT_SUMMARY, format, Instant.now());
    }

    @Override
    public UserActivityReportData fetchUserActivityData(Instant startDate, Instant endDate) {
        List<User> users = userRepository.findUsersForReport(startDate, endDate);
        List<Reservation> reservations = reservationRepository.findReservationsForReport(startDate, endDate);

        Map<Long, Long> reservationsCountByUser = reservations.stream()
                .filter(r -> r.getUser() != null && r.getUser().getId() != null)
                .collect(Collectors.groupingBy(r -> r.getUser().getId(), Collectors.counting()));

        Map<Long, BigDecimal> spendByUser = reservations.stream()
                .filter(r -> r.getUser() != null && r.getUser().getId() != null && r.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getUser().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Reservation::getTotalAmount, BigDecimal::add)
                ));

        long totalUsers = users.size();
        long activeUsers = users.stream().filter(User::isActive).count();
        long inactiveUsers = totalUsers - activeUsers;

        Map<String, Long> usersByRole = users.stream()
                .collect(Collectors.groupingBy(u -> u.getRole() != null ? u.getRole().name() : "UNKNOWN", TreeMap::new, Collectors.counting()));

        List<UserActivityReportData.UserActivityItem> items = users.stream()
                .map(u -> new UserActivityReportData.UserActivityItem(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getFullName(),
                        u.getRole() != null ? u.getRole().name() : "USER",
                        u.isActive(),
                        reservationsCountByUser.getOrDefault(u.getId(), 0L),
                        spendByUser.getOrDefault(u.getId(), BigDecimal.ZERO),
                        u.getCreatedAt()
                ))
                .sorted(Comparator.comparing(UserActivityReportData.UserActivityItem::getTotalSpent).reversed()
                        .thenComparing(UserActivityReportData.UserActivityItem::getUserId))
                .toList();

        return new UserActivityReportData(startDate, endDate, totalUsers, activeUsers, inactiveUsers, usersByRole, items);
    }

    @Override
    public BookingTrendsReportData fetchBookingTrendsData(Instant startDate, Instant endDate) {
        List<Reservation> reservations = reservationRepository.findReservationsForReport(startDate, endDate);

        long totalBookings = reservations.size();

        Map<String, Long> bookingsByStatus = reservations.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus() != null ? r.getStatus().name() : "UNKNOWN", TreeMap::new, Collectors.counting()));

        Map<String, Long> bookingsByLot = reservations.stream()
                .map(r -> {
                    if (r.getParkingSpot() != null && r.getParkingSpot().getParkingLot() != null) {
                        return r.getParkingSpot().getParkingLot().getName();
                    }
                    return "Unassigned Lot";
                })
                .collect(Collectors.groupingBy(name -> name, TreeMap::new, Collectors.counting()));

        Map<String, Long> dailyBookings = reservations.stream()
                .filter(r -> r.getStartTime() != null)
                .collect(Collectors.groupingBy(r -> DAY_FORMATTER.format(r.getStartTime()), TreeMap::new, Collectors.counting()));

        BigDecimal totalRevenue = reservations.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalDurationMinutes = reservations.stream()
                .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                .mapToDouble(r -> Duration.between(r.getStartTime(), r.getEndTime()).toMinutes())
                .sum();

        double avgDurationHours = totalBookings > 0 ? (totalDurationMinutes / 60.0) / totalBookings : 0.0;

        List<BookingTrendsReportData.BookingTrendItem> items = reservations.stream()
                .map(r -> {
                    String userEmail = r.getUser() != null ? r.getUser().getEmail() : "N/A";
                    String customerName = r.getUser() != null && r.getUser().getFullName() != null ? r.getUser().getFullName() : "N/A";
                    String lotName = (r.getParkingSpot() != null && r.getParkingSpot().getParkingLot() != null)
                            ? r.getParkingSpot().getParkingLot().getName()
                            : "N/A";
                    String spotNumber = r.getParkingSpot() != null ? r.getParkingSpot().getSpotNumber() : "N/A";
                    String plate = r.getVehicle() != null ? r.getVehicle().getLicensePlate() : "N/A";
                    String status = r.getStatus() != null ? r.getStatus().name() : "CONFIRMED";

                    return new BookingTrendsReportData.BookingTrendItem(
                            r.getId(),
                            userEmail,
                            customerName,
                            lotName,
                            spotNumber,
                            plate,
                            r.getStartTime(),
                            r.getEndTime(),
                            r.getTotalAmount(),
                            status
                    );
                })
                .sorted(Comparator.comparing(BookingTrendsReportData.BookingTrendItem::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new BookingTrendsReportData(startDate, endDate, totalBookings, bookingsByStatus, bookingsByLot, dailyBookings, totalRevenue, avgDurationHours, items);
    }

    @Override
    public PaymentSummaryReportData fetchPaymentSummaryData(Instant startDate, Instant endDate) {
        List<Payment> payments = paymentRepository.findPaymentsForReport(startDate, endDate);

        long totalTransactions = payments.size();

        BigDecimal totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> paymentsByStatus = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus() != null ? p.getStatus().name() : "UNKNOWN", TreeMap::new, Collectors.counting()));

        Map<String, BigDecimal> revenueByMethod = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "OTHER",
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));

        Map<String, BigDecimal> revenueByPeriod = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .collect(Collectors.groupingBy(
                        p -> p.getPaidAt() != null ? MONTH_FORMATTER.format(p.getPaidAt()) : (p.getCreatedAt() != null ? MONTH_FORMATTER.format(p.getCreatedAt()) : "N/A"),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));

        List<PaymentSummaryReportData.PaymentSummaryItem> items = payments.stream()
                .map(p -> {
                    Reservation res = p.getReservation();
                    String customerEmail = (res != null && res.getUser() != null) ? res.getUser().getEmail() : "N/A";
                    Long resId = res != null ? res.getId() : null;
                    String method = p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "N/A";
                    String status = p.getStatus() != null ? p.getStatus().name() : "N/A";

                    return new PaymentSummaryReportData.PaymentSummaryItem(
                            p.getId(),
                            p.getTransactionId() != null ? p.getTransactionId() : "N/A",
                            resId,
                            customerEmail,
                            p.getAmount(),
                            p.getCurrency() != null ? p.getCurrency() : "USD",
                            method,
                            status,
                            p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt()
                    );
                })
                .sorted(Comparator.comparing(PaymentSummaryReportData.PaymentSummaryItem::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new PaymentSummaryReportData(startDate, endDate, totalTransactions, totalRevenue, paymentsByStatus, revenueByMethod, revenueByPeriod, items);
    }

    private ReportType resolveReportType(String typeStr) {
        String normalized = typeStr.trim().replaceAll("[_-]", "").toLowerCase();
        return switch (normalized) {
            case "monthlyrevenue", "revenue", "paymentsummary", "payments" -> ReportType.PAYMENT_SUMMARY;
            case "useractivity", "users", "activity" -> ReportType.USER_ACTIVITY;
            case "bookingtrends", "bookings", "reservations" -> ReportType.BOOKING_TRENDS;
            default -> {
                try {
                    yield ReportType.valueOf(typeStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Unsupported reportType: '" + typeStr + "'. Expected monthlyRevenue, userActivity, bookingTrends, or paymentSummary.");
                }
            }
        };
    }

    private ReportFormat resolveReportFormat(String formatStr) {
        if (formatStr == null || formatStr.isBlank()) {
            return ReportFormat.CSV;
        }
        try {
            return ReportFormat.valueOf(formatStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported report format: '" + formatStr + "'. Expected CSV or PDF.");
        }
    }

    private Instant parseDate(String dateStr, boolean isEndOfDay) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(dateStr.trim());
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr.trim());
                if (isEndOfDay) {
                    return localDate.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();
                } else {
                    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                }
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date format: '" + dateStr + "'. Expected ISO-8601 (e.g., '2025-01-01' or '2025-01-01T00:00:00Z').");
            }
        }
    }

    private ReportExporter getExporter(ReportFormat format) {
        ReportExporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new IllegalArgumentException("No exporter registered for format: " + format);
        }
        return exporter;
    }
}
