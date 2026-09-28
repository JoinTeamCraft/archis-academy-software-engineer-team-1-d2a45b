package tech.lokum.parkinglot.entity;

/**
 * Roles for access control across the application.
 */
public enum Role {
    ADMIN,
    MANAGER,
    USER,
    OPERATOR,
    CUSTOMER;

    /**
     * Returns the Spring Security authority format (e.g., "ROLE_ADMIN").
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
