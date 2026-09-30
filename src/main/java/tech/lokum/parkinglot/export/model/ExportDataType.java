package tech.lokum.parkinglot.export.model;

import tech.lokum.parkinglot.exception.BadRequestException;

import java.util.Arrays;

/**
 * Data entities supported for export.
 */
public enum ExportDataType {
    BOOKINGS("bookings", "reservations"),
    USERS("users", "accounts"),
    PAYMENTS("payments", "transactions"),
    PARKING_LOTS("parking_lots", "parkinglots", "lots");

    private final String[] aliases;

    ExportDataType(String... aliases) {
        this.aliases = aliases;
    }

    /**
     * Resolves a case-insensitive name or alias into an {@link ExportDataType}.
     *
     * @param value data type string
     * @return matching ExportDataType
     * @throws BadRequestException if the data type is unrecognized
     */
    public static ExportDataType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Data type is required (supported: bookings, users, payments, parking_lots)");
        }
        String normalized = value.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        for (ExportDataType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
            for (String alias : type.aliases) {
                if (alias.equalsIgnoreCase(normalized)) {
                    return type;
                }
            }
        }
        throw new BadRequestException("Unsupported data type: '" + value + "'. Supported: "
                + Arrays.toString(new String[]{"bookings", "users", "payments", "parking_lots"}));
    }
}
