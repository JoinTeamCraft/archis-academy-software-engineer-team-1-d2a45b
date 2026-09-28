package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.lokum.parkinglot.dto.CreateReservationRequest;
import tech.lokum.parkinglot.dto.ErrorResponse;
import tech.lokum.parkinglot.dto.ReservationResponse;
import tech.lokum.parkinglot.dto.ReservationStatusResponse;
import tech.lokum.parkinglot.dto.UpdateReservationRequest;
import tech.lokum.parkinglot.dto.UpdateReservationStatusRequest;
import tech.lokum.parkinglot.service.ReservationService;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing parking reservations.
 */
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Endpoints for booking, updating, and querying parking spot reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Creates a new parking spot reservation.
     *
     * @param request reservation payload containing vehicleId, parkingSpotId, startTime, endTime
     * @return 201 Created with the created reservation details
     */
    @PostMapping
    @Operation(
        summary = "Create parking reservation",
        description = "Creates a new parking reservation for a vehicle at a designated spot. Checks availability, spot compatibility, and double-booking."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Reservation successfully created and confirmed",
        content = @Content(schema = @Schema(implementation = ReservationResponse.class)),
        headers = @Header(name = "Location", description = "URI of the newly created reservation", schema = @Schema(type = "string"))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed (e.g. invalid time range, vehicle incompatible)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Referenced user, vehicle or parking spot not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "Conflict: parking spot is already reserved for overlapping time or unavailable",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<ReservationResponse> createReservation(
        @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationResponse response = reservationService.createReservation(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Retrieves a reservation by ID.
     *
     * @param id reservation ID
     * @return reservation details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID", description = "Retrieves reservation details for the given ID.")
    @ApiResponse(responseCode = "200", description = "Reservation found", content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all reservations with optional user ID filter and pagination support.
     *
     * @param userId optional user ID filter
     * @param page optional page number (0-based)
     * @param size optional page size
     * @return list of reservation DTOs
     */
    @GetMapping
    @Operation(summary = "Get all reservations", description = "Retrieves reservations with optional user filtering and pagination.")
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved reservations",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservationResponse.class)))
    )
    public ResponseEntity<List<ReservationResponse>> getReservations(
        @Parameter(description = "Optional filter by user ID")
        @RequestParam(name = "userId", required = false) Long userId,
        @Parameter(description = "Zero-based page index")
        @RequestParam(name = "page", required = false) Integer page,
        @Parameter(description = "Page size")
        @RequestParam(name = "size", required = false) Integer size
    ) {
        if (page != null || size != null) {
            int pageIndex = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 20;
            Pageable pageable = PageRequest.of(pageIndex, pageSize);

            Page<ReservationResponse> reservationPage = (userId != null)
                ? reservationService.getReservationsByUserId(userId, pageable)
                : reservationService.getAllReservations(pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(reservationPage.getTotalElements()));
            headers.add("X-Total-Pages", String.valueOf(reservationPage.getTotalPages()));
            headers.add("X-Current-Page", String.valueOf(reservationPage.getNumber()));
            headers.add("X-Page-Size", String.valueOf(reservationPage.getSize()));

            return ResponseEntity.ok().headers(headers).body(reservationPage.getContent());
        }

        List<ReservationResponse> reservations = (userId != null)
            ? reservationService.getReservationsByUserId(userId)
            : reservationService.getAllReservations();

        return ResponseEntity.ok(reservations);
    }

    /**
     * Updates an existing reservation.
     *
     * @param id reservation ID
     * @param request update payload
     * @return updated reservation details
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update reservation", description = "Updates an existing reservation's time window, spot, vehicle, or status.")
    @ApiResponse(responseCode = "200", description = "Reservation successfully updated", content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request or completed reservation", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Overlap conflict on reschedule", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReservationResponse> updateReservation(
        @PathVariable Long id,
        @Valid @RequestBody UpdateReservationRequest request
    ) {
        ReservationResponse updated = reservationService.updateReservation(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Updates the status of an existing parking reservation (e.g., cancel or confirm).
     *
     * @param reservationId ID of the reservation to update
     * @param request payload containing the new status
     * @return 200 OK with the updated reservation ID and status
     */
    @PutMapping("/{reservationId}/status")
    @Operation(
        summary = "Update reservation status",
        description = "Updates the status of an existing reservation (e.g., cancel or confirm)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Reservation status successfully updated",
        content = @Content(schema = @Schema(implementation = ReservationStatusResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed or illegal status transition",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Reservation not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "Conflict: parking spot is already reserved for this time window",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<ReservationStatusResponse> updateReservationStatus(
        @PathVariable Long reservationId,
        @Valid @RequestBody UpdateReservationStatusRequest request
    ) {
        ReservationStatusResponse response = reservationService.updateReservationStatus(reservationId, request.status());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels / deletes a reservation.
     *
     * @param id reservation ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel / Delete reservation", description = "Cancels or deletes a reservation and releases the parking spot.")
    @ApiResponse(responseCode = "204", description = "Reservation successfully deleted / cancelled")
    @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
