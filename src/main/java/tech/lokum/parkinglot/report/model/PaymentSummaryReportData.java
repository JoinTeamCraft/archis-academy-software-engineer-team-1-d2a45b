package tech.lokum.parkinglot.report.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Aggregated data payload for payment summary reporting.
 */
public class PaymentSummaryReportData {

    private final Instant periodStart;
    private final Instant periodEnd;
    private final long totalTransactions;
    private final BigDecimal totalRevenue;
    private final Map<String, Long> paymentsByStatus;
    private final Map<String, BigDecimal> revenueByMethod;
    private final Map<String, BigDecimal> revenueByPeriod;
    private final List<PaymentSummaryItem> items;

    public PaymentSummaryReportData(
            Instant periodStart,
            Instant periodEnd,
            long totalTransactions,
            BigDecimal totalRevenue,
            Map<String, Long> paymentsByStatus,
            Map<String, BigDecimal> revenueByMethod,
            Map<String, BigDecimal> revenueByPeriod,
            List<PaymentSummaryItem> items
    ) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalTransactions = totalTransactions;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.paymentsByStatus = paymentsByStatus != null ? paymentsByStatus : Collections.emptyMap();
        this.revenueByMethod = revenueByMethod != null ? revenueByMethod : Collections.emptyMap();
        this.revenueByPeriod = revenueByPeriod != null ? revenueByPeriod : Collections.emptyMap();
        this.items = items != null ? items : Collections.emptyList();
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public Map<String, Long> getPaymentsByStatus() {
        return paymentsByStatus;
    }

    public Map<String, BigDecimal> getRevenueByMethod() {
        return revenueByMethod;
    }

    public Map<String, BigDecimal> getRevenueByPeriod() {
        return revenueByPeriod;
    }

    public List<PaymentSummaryItem> getItems() {
        return items;
    }

    public static class PaymentSummaryItem {
        private final Long paymentId;
        private final String transactionId;
        private final Long reservationId;
        private final String customerEmail;
        private final BigDecimal amount;
        private final String currency;
        private final String paymentMethod;
        private final String status;
        private final Instant paidAt;

        public PaymentSummaryItem(
                Long paymentId,
                String transactionId,
                Long reservationId,
                String customerEmail,
                BigDecimal amount,
                String currency,
                String paymentMethod,
                String status,
                Instant paidAt
        ) {
            this.paymentId = paymentId;
            this.transactionId = transactionId;
            this.reservationId = reservationId;
            this.customerEmail = customerEmail;
            this.amount = amount != null ? amount : BigDecimal.ZERO;
            this.currency = currency != null ? currency : "USD";
            this.paymentMethod = paymentMethod;
            this.status = status;
            this.paidAt = paidAt;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public String getCustomerEmail() {
            return customerEmail;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public String getStatus() {
            return status;
        }

        public Instant getPaidAt() {
            return paidAt;
        }
    }
}
