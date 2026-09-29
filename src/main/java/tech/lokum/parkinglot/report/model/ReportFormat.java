package tech.lokum.parkinglot.report.model;

/**
 * Enumeration representing export formats for generated reports.
 */
public enum ReportFormat {
    CSV("text/csv; charset=UTF-8", "csv"),
    PDF("application/pdf", "pdf");

    private final String mediaType;
    private final String extension;

    ReportFormat(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getExtension() {
        return extension;
    }
}
