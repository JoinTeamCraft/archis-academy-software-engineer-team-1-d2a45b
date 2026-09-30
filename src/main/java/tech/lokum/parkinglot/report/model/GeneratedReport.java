package tech.lokum.parkinglot.report.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Model representing an in-memory generated report ready for streaming or saving.
 */
public class GeneratedReport {

    private final String fileName;
    private final String contentType;
    private final byte[] content;
    private final ReportType reportType;
    private final ReportFormat reportFormat;
    private final Instant generatedAt;

    public GeneratedReport(String fileName, String contentType, byte[] content, ReportType reportType, ReportFormat reportFormat, Instant generatedAt) {
        this.fileName = Objects.requireNonNull(fileName, "fileName cannot be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.reportType = Objects.requireNonNull(reportType, "reportType cannot be null");
        this.reportFormat = Objects.requireNonNull(reportFormat, "reportFormat cannot be null");
        this.generatedAt = generatedAt != null ? generatedAt : Instant.now();
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public ReportFormat getReportFormat() {
        return reportFormat;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public int getSize() {
        return content.length;
    }
}
