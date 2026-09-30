package tech.lokum.parkinglot.export.model;

import tech.lokum.parkinglot.exception.BadRequestException;

/**
 * Output formats supported for data export.
 */
public enum ExportFormat {
    CSV("text/csv; charset=UTF-8", ".csv"),
    JSON("application/json; charset=UTF-8", ".json");

    private final String mediaType;
    private final String extension;

    ExportFormat(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Resolves a format string case-insensitively, defaulting to CSV if null or blank.
     *
     * @param value format string
     * @return matching ExportFormat (defaults to CSV)
     */
    public static ExportFormat from(String value) {
        if (value == null || value.isBlank()) {
            return CSV;
        }
        String normalized = value.trim().toUpperCase();
        try {
            return ExportFormat.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported export format: '" + value + "'. Supported formats: CSV, JSON");
        }
    }
}
