package tech.lokum.parkinglot.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.report.entity.ReportMetadata;

import java.util.List;

/**
 * Spring Data JPA repository for persisting and querying {@link ReportMetadata}.
 */
@Repository
public interface ReportMetadataRepository extends JpaRepository<ReportMetadata, Long> {

    List<ReportMetadata> findByReportType(String reportType);

    List<ReportMetadata> findByRequestedBy(String requestedBy);

    List<ReportMetadata> findByStatus(String status);
}
