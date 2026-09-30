package tech.lokum.parkinglot.report.model;

/**
 * Enumeration representing supported reporting categories.
 */
public enum ReportType {
    USER_ACTIVITY("User Activity"),
    BOOKING_TRENDS("Booking Trends"),
    PAYMENT_SUMMARY("Payment Summary");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
