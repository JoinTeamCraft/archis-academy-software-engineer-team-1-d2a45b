package tech.lokum.parkinglot.report.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Aggregated data payload for booking trends reporting.
 */
public class BookingTrendsReportData {

    private final Instant periodStart;
    private final Instant periodEnd;
    private final long totalBookings;
    private final Map<String, Long> bookingsByStatus;
    private final Map<String, Long> bookingsByLot;
    private final Map<String, Long> dailyBookings;
    private final BigDecimal totalRevenue;
    private final double averageDurationHours;
    private final List<BookingTrendItem> items;

    public BookingTrendsReportData(
            Instant periodStart,
            Instant periodEnd,
            long totalBookings,
            Map<String, Long> bookingsByStatus,
            Map<String, Long> bookingsByLot,
            Map<String, Long> dailyBookings,
            BigDecimal totalRevenue,
            double averageDurationHours,
            List<BookingTrendItem> items
    ) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalBookings = totalBookings;
        this.bookingsByStatus = bookingsByStatus != null ? bookingsByStatus : Collections.emptyMap();
        this.bookingsByLot = bookingsByLot != null ? bookingsByLot : Collections.emptyMap();
        this.dailyBookings = dailyBookings != null ? dailyBookings : Collections.emptyMap();
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.averageDurationHours = averageDurationHours;
        this.items = items != null ? items : Collections.emptyList();
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public Map<String, Long> getBookingsByStatus() {
        return bookingsByStatus;
    }

    public Map<String, Long> getBookingsByLot() {
        return bookingsByLot;
    }

    public Map<String, Long> getDailyBookings() {
        return dailyBookings;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public double getAverageDurationHours() {
        return averageDurationHours;
    }

    public List<BookingTrendItem> getItems() {
        return items;
    }

    public static class BookingTrendItem {
        private final Long reservationId;
        private final String userEmail;
        private final String customerName;
        private final String lotName;
        private final String spotNumber;
        private final String vehiclePlate;
        private final Instant startTime;
        private final Instant endTime;
        private final BigDecimal totalAmount;
        private final String status;

        public BookingTrendItem(
                Long reservationId,
                String userEmail,
                String customerName,
                String lotName,
                String spotNumber,
                String vehiclePlate,
                Instant startTime,
                Instant endTime,
                BigDecimal totalAmount,
                String status
        ) {
            this.reservationId = reservationId;
            this.userEmail = userEmail;
            this.customerName = customerName;
            this.lotName = lotName;
            this.spotNumber = spotNumber;
            this.vehiclePlate = vehiclePlate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
            this.status = status;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getLotName() {
            return lotName;
        }

        public String getSpotNumber() {
            return spotNumber;
        }

        public String getVehiclePlate() {
            return vehiclePlate;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public String getStatus() {
            return status;
        }
    }
}
