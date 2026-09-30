package tech.lokum.parkinglot.report.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Aggregated data payload for user activity reporting.
 */
public class UserActivityReportData {

    private final Instant periodStart;
    private final Instant periodEnd;
    private final long totalUsers;
    private final long activeUsers;
    private final long inactiveUsers;
    private final Map<String, Long> usersByRole;
    private final List<UserActivityItem> items;

    public UserActivityReportData(
            Instant periodStart,
            Instant periodEnd,
            long totalUsers,
            long activeUsers,
            long inactiveUsers,
            Map<String, Long> usersByRole,
            List<UserActivityItem> items
    ) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.inactiveUsers = inactiveUsers;
        this.usersByRole = usersByRole != null ? usersByRole : Collections.emptyMap();
        this.items = items != null ? items : Collections.emptyList();
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public long getInactiveUsers() {
        return inactiveUsers;
    }

    public Map<String, Long> getUsersByRole() {
        return usersByRole;
    }

    public List<UserActivityItem> getItems() {
        return items;
    }

    public static class UserActivityItem {
        private final Long userId;
        private final String username;
        private final String email;
        private final String fullName;
        private final String role;
        private final boolean active;
        private final long totalReservations;
        private final BigDecimal totalSpent;
        private final Instant registeredAt;

        public UserActivityItem(
                Long userId,
                String username,
                String email,
                String fullName,
                String role,
                boolean active,
                long totalReservations,
                BigDecimal totalSpent,
                Instant registeredAt
        ) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.active = active;
            this.totalReservations = totalReservations;
            this.totalSpent = totalSpent != null ? totalSpent : BigDecimal.ZERO;
            this.registeredAt = registeredAt;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getFullName() {
            return fullName;
        }

        public String getRole() {
            return role;
        }

        public boolean isActive() {
            return active;
        }

        public long getTotalReservations() {
            return totalReservations;
        }

        public BigDecimal getTotalSpent() {
            return totalSpent;
        }

        public Instant getRegisteredAt() {
            return registeredAt;
        }
    }
}
