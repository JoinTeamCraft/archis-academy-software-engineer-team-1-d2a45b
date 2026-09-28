package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Standardized paginated response wrapper for list endpoints.
 *
 * @param <T> element type in the page
 */
@Schema(description = "Paginated list response wrapper")
public record PageResponse<T>(
    @Schema(description = "List of elements in the current page")
    List<T> content,

    @Schema(description = "Current page index (0-based)", example = "0")
    int page,

    @Schema(description = "Page size (number of items requested per page)", example = "20")
    int size,

    @Schema(description = "Total number of elements across all pages", example = "100")
    long totalElements,

    @Schema(description = "Total number of pages", example = "5")
    int totalPages,

    @Schema(description = "True if this is the first page", example = "true")
    boolean first,

    @Schema(description = "True if this is the last page", example = "false")
    boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
