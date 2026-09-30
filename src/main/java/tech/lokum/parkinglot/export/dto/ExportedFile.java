package tech.lokum.parkinglot.export.dto;

/**
 * Encapsulates the generated binary export file, media type, and row statistics.
 */
public class ExportedFile {

    private final String fileName;
    private final String contentType;
    private final byte[] content;
    private final long recordCount;

    public ExportedFile(String fileName, String contentType, byte[] content, long recordCount) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
        this.recordCount = recordCount;
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

    public long getRecordCount() {
        return recordCount;
    }
}
