package tech.lokum.parkinglot.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.export.dto.DataExportRequest;
import tech.lokum.parkinglot.export.dto.ExportedFile;
import tech.lokum.parkinglot.export.model.ExportDataType;
import tech.lokum.parkinglot.export.model.ExportFormat;
import tech.lokum.parkinglot.repository.ParkingLotRepository;
import tech.lokum.parkinglot.repository.PaymentRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service implementation for fetching and formatting system datasets into CSV or JSON.
 * Processes data in batches of 500 to handle large datasets efficiently with minimal memory overhead.
 */
@Service
@Transactional(readOnly = true)
public class DataExportServiceImpl implements DataExportService {

    private static final int BATCH_SIZE = 500;
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DataExportServiceImpl(
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            ParkingLotRepository parkingLotRepository,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public ExportedFile exportData(DataExportRequest request) {
        ExportDataType dataType = ExportDataType.from(request.getDataType());
        ExportFormat format = ExportFormat.from(request.getFormat());

        ByteArrayOutputStream baos = new ByteArrayOutputStream(32 * 1024);
        long recordCount = streamExportData(request, baos);

        String timestamp = FILE_TIMESTAMP_FORMATTER.format(Instant.now());
        String fileName = dataType.name().toLowerCase() + "_export_" + timestamp + format.getExtension();

        return new ExportedFile(fileName, format.getMediaType(), baos.toByteArray(), recordCount);
    }

    @Override
    public long streamExportData(DataExportRequest request, OutputStream outputStream) {
        ExportDataType dataType = ExportDataType.from(request.getDataType());
        ExportFormat format = ExportFormat.from(request.getFormat());

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            long count;
            switch (dataType) {
                case BOOKINGS -> count = streamBookings(request, format, writer);
                case USERS -> count = streamUsers(request, format, writer);
                case PAYMENTS -> count = streamPayments(request, format, writer);
                case PARKING_LOTS -> count = streamParkingLots(request, format, writer);
                default -> throw new IllegalStateException("Unhandled data type: " + dataType);
            }
            writer.flush();
            return count;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to stream export data: " + ex.getMessage(), ex);
        }
    }

    private long streamBookings(DataExportRequest request, ExportFormat format, BufferedWriter writer) throws IOException {
        long count = 0;
        int page = 0;
        Page<Reservation> pageResult;

        if (format == ExportFormat.CSV) {
            writer.write("id,user_id,user_email,user_name,vehicle_plate,vehicle_type,parking_lot,spot_number,start_time,end_time,total_amount,status,created_at\n");
        } else {
            writer.write("[\n");
        }

        boolean first = true;
        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.DESC, "id"));
            pageResult = reservationRepository.findAll(pageable);

            for (Reservation res : pageResult.getContent()) {
                if (matchesReservationFilters(res, request)) {
                    if (format == ExportFormat.CSV) {
                        writer.write(toBookingCsvRow(res));
                    } else {
                        if (!first) {
                            writer.write(",\n");
                        }
                        writer.write("  " + objectMapper.writeValueAsString(toBookingMap(res)));
                        first = false;
                    }
                    count++;
                }
            }
            writer.flush();
            page++;
        } while (pageResult.hasNext());

        if (format == ExportFormat.JSON) {
            writer.write("\n]");
        }
        return count;
    }

    private long streamUsers(DataExportRequest request, ExportFormat format, BufferedWriter writer) throws IOException {
        long count = 0;
        int page = 0;
        Page<User> pageResult;

        if (format == ExportFormat.CSV) {
            writer.write("id,username,email,full_name,phone_number,role,is_active,created_at\n");
        } else {
            writer.write("[\n");
        }

        boolean first = true;
        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            pageResult = userRepository.findAll(pageable);

            for (User user : pageResult.getContent()) {
                if (format == ExportFormat.CSV) {
                    writer.write(toUserCsvRow(user));
                } else {
                    if (!first) {
                        writer.write(",\n");
                    }
                    writer.write("  " + objectMapper.writeValueAsString(toUserMap(user)));
                    first = false;
                }
                count++;
            }
            writer.flush();
            page++;
        } while (pageResult.hasNext());

        if (format == ExportFormat.JSON) {
            writer.write("\n]");
        }
        return count;
    }

    private long streamPayments(DataExportRequest request, ExportFormat format, BufferedWriter writer) throws IOException {
        long count = 0;
        int page = 0;
        Page<Payment> pageResult;

        if (format == ExportFormat.CSV) {
            writer.write("id,reservation_id,amount,currency,payment_method,status,transaction_id,paid_at,created_at\n");
        } else {
            writer.write("[\n");
        }

        boolean first = true;
        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.DESC, "id"));
            pageResult = paymentRepository.findAll(pageable);

            for (Payment payment : pageResult.getContent()) {
                if (format == ExportFormat.CSV) {
                    writer.write(toPaymentCsvRow(payment));
                } else {
                    if (!first) {
                        writer.write(",\n");
                    }
                    writer.write("  " + objectMapper.writeValueAsString(toPaymentMap(payment)));
                    first = false;
                }
                count++;
            }
            writer.flush();
            page++;
        } while (pageResult.hasNext());

        if (format == ExportFormat.JSON) {
            writer.write("\n]");
        }
        return count;
    }

    private long streamParkingLots(DataExportRequest request, ExportFormat format, BufferedWriter writer) throws IOException {
        long count = 0;
        int page = 0;
        Page<ParkingLot> pageResult;

        if (format == ExportFormat.CSV) {
            writer.write("id,name,location,capacity,hourly_rate,is_active,created_at\n");
        } else {
            writer.write("[\n");
        }

        boolean first = true;
        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            pageResult = parkingLotRepository.findAll(pageable);

            for (ParkingLot lot : pageResult.getContent()) {
                if (format == ExportFormat.CSV) {
                    writer.write(toParkingLotCsvRow(lot));
                } else {
                    if (!first) {
                        writer.write(",\n");
                    }
                    writer.write("  " + objectMapper.writeValueAsString(toParkingLotMap(lot)));
                    first = false;
                }
                count++;
            }
            writer.flush();
            page++;
        } while (pageResult.hasNext());

        if (format == ExportFormat.JSON) {
            writer.write("\n]");
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Row Formatting & Mapping
    // -------------------------------------------------------------------------

    private String toBookingCsvRow(Reservation r) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                r.getId(),
                r.getUser() != null ? r.getUser().getId() : "",
                escapeCsv(r.getUser() != null ? r.getUser().getEmail() : ""),
                escapeCsv(r.getUser() != null ? r.getUser().getFullName() : ""),
                escapeCsv(r.getVehicle() != null ? r.getVehicle().getLicensePlate() : ""),
                r.getVehicle() != null && r.getVehicle().getVehicleType() != null ? r.getVehicle().getVehicleType().name() : "",
                escapeCsv(r.getParkingSpot() != null && r.getParkingSpot().getParkingLot() != null ? r.getParkingSpot().getParkingLot().getName() : ""),
                escapeCsv(r.getParkingSpot() != null ? r.getParkingSpot().getSpotNumber() : ""),
                r.getStartTime() != null ? r.getStartTime().toString() : "",
                r.getEndTime() != null ? r.getEndTime().toString() : "",
                r.getTotalAmount() != null ? r.getTotalAmount().toPlainString() : "0.00",
                r.getStatus() != null ? r.getStatus().name() : "",
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> toBookingMap(Reservation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("userId", r.getUser() != null ? r.getUser().getId() : null);
        map.put("userEmail", r.getUser() != null ? r.getUser().getEmail() : null);
        map.put("userName", r.getUser() != null ? r.getUser().getFullName() : null);
        map.put("vehiclePlate", r.getVehicle() != null ? r.getVehicle().getLicensePlate() : null);
        map.put("vehicleType", r.getVehicle() != null && r.getVehicle().getVehicleType() != null ? r.getVehicle().getVehicleType().name() : null);
        map.put("parkingLot", r.getParkingSpot() != null && r.getParkingSpot().getParkingLot() != null ? r.getParkingSpot().getParkingLot().getName() : null);
        map.put("spotNumber", r.getParkingSpot() != null ? r.getParkingSpot().getSpotNumber() : null);
        map.put("startTime", r.getStartTime() != null ? r.getStartTime().toString() : null);
        map.put("endTime", r.getEndTime() != null ? r.getEndTime().toString() : null);
        map.put("totalAmount", r.getTotalAmount() != null ? r.getTotalAmount().toPlainString() : "0.00");
        map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return map;
    }

    private String toUserCsvRow(User u) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                u.getId(),
                escapeCsv(u.getUsername()),
                escapeCsv(u.getEmail()),
                escapeCsv(u.getFullName()),
                escapeCsv(u.getPhoneNumber()),
                u.getRole() != null ? u.getRole().name() : "",
                u.isActive(),
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("email", u.getEmail());
        map.put("fullName", u.getFullName());
        map.put("phoneNumber", u.getPhoneNumber());
        map.put("role", u.getRole() != null ? u.getRole().name() : null);
        map.put("isActive", u.isActive());
        map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return map;
    }

    private String toPaymentCsvRow(Payment p) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                p.getId(),
                p.getReservation() != null ? p.getReservation().getId() : "",
                p.getAmount() != null ? p.getAmount().toPlainString() : "0.00",
                escapeCsv(p.getCurrency()),
                p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "",
                p.getStatus() != null ? p.getStatus().name() : "",
                escapeCsv(p.getTransactionId()),
                p.getPaidAt() != null ? p.getPaidAt().toString() : "",
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> toPaymentMap(Payment p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("reservationId", p.getReservation() != null ? p.getReservation().getId() : null);
        map.put("amount", p.getAmount() != null ? p.getAmount().toPlainString() : "0.00");
        map.put("currency", p.getCurrency());
        map.put("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null);
        map.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        map.put("transactionId", p.getTransactionId());
        map.put("paidAt", p.getPaidAt() != null ? p.getPaidAt().toString() : null);
        map.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return map;
    }

    private String toParkingLotCsvRow(ParkingLot lot) {
        return String.format("%s,%s,%s,%s,%s,%s,%s\n",
                lot.getId(),
                escapeCsv(lot.getName()),
                escapeCsv(lot.getLocation()),
                lot.getCapacity(),
                lot.getHourlyRate() != null ? lot.getHourlyRate().toPlainString() : "0.00",
                lot.isActive(),
                lot.getCreatedAt() != null ? lot.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> toParkingLotMap(ParkingLot lot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", lot.getId());
        map.put("name", lot.getName());
        map.put("location", lot.getLocation());
        map.put("capacity", lot.getCapacity());
        map.put("hourlyRate", lot.getHourlyRate() != null ? lot.getHourlyRate().toPlainString() : "0.00");
        map.put("isActive", lot.isActive());
        map.put("createdAt", lot.getCreatedAt() != null ? lot.getCreatedAt().toString() : null);
        return map;
    }

    private boolean matchesReservationFilters(Reservation res, DataExportRequest req) {
        if (req.getStartDate() != null && res.getStartTime() != null && res.getStartTime().isBefore(req.getStartDate())) {
            return false;
        }
        if (req.getEndDate() != null && res.getStartTime() != null && res.getStartTime().isAfter(req.getEndDate())) {
            return false;
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            return res.getStatus() != null && res.getStatus().name().equalsIgnoreCase(req.getStatus().trim());
        }
        return true;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
