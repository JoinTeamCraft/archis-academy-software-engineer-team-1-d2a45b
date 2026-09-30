package tech.lokum.parkinglot.export.service;

import tech.lokum.parkinglot.export.dto.DataExportRequest;
import tech.lokum.parkinglot.export.dto.ExportedFile;

import java.io.OutputStream;

/**
 * Service providing efficient data retrieval, batching, and formatting for data exports.
 */
public interface DataExportService {

    /**
     * Exports the requested dataset into an in-memory {@link ExportedFile}.
     *
     * @param request export parameters (data type, format, filters)
     * @return exported file containing file name, media type, binary content, and record count
     */
    ExportedFile exportData(DataExportRequest request);

    /**
     * Streams the requested dataset directly into the provided {@link OutputStream}.
     * Uses batching and incremental flushing to guarantee low memory consumption on large datasets.
     *
     * @param request export parameters
     * @param outputStream target destination stream
     * @return total number of records exported
     */
    long streamExportData(DataExportRequest request, OutputStream outputStream);
}
