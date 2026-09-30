package tech.lokum.parkinglot.report.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import tech.lokum.parkinglot.entity.BaseEntity;

/**
 * JPA entity representing persisted metadata and binary content of generated reports.
 */
@Entity
@Table(
        name = "report_metadata",
        indexes = {
                @Index(name = "idx_report_type", columnList = "report_type"),
                @Index(name = "idx_report_status", columnList = "status"),
                @Index(name = "idx_report_requested_by", columnList = "requested_by")
        }
)
public class ReportMetadata extends BaseEntity {

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "format", nullable = false, length = 20)
    private String format;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "generated";

    @Column(name = "start_date", length = 50)
    private String startDate;

    @Column(name = "end_date", length = 50)
    private String endDate;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "download_link", length = 255)
    private String downloadLink;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false)
    private byte[] content;

    public ReportMetadata() {
    }

    public ReportMetadata(
            String reportType,
            String format,
            String status,
            String startDate,
            String endDate,
            String fileName,
            String contentType,
            String downloadLink,
            String requestedBy,
            byte[] content
    ) {
        this.reportType = reportType;
        this.format = format;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fileName = fileName;
        this.contentType = contentType;
        this.downloadLink = downloadLink;
        this.requestedBy = requestedBy;
        this.content = content;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
