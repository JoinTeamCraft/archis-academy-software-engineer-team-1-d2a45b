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
import tech.lokum.parkinglot.dto.CreateParkingLotRequest;
import tech.lokum.parkinglot.dto.ErrorResponse;
import tech.lokum.parkinglot.dto.ParkingLotResponse;
import tech.lokum.parkinglot.dto.UpdateParkingLotRequest;
import tech.lokum.parkinglot.service.ParkingLotService;

import java.net.URI;
import java.util.List;

/**
 * REST controller for managing parking lots and retrieving lot availability.
 */
@RestController
@RequestMapping("/api/parking-lots")
@Tag(name = "Parking Lots", description = "Endpoints for parking lot discovery and management")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    /**
     * Retrieves all parking lots with optional pagination support.
     *
     * @param page optional page number (0-based)
     * @param size optional page size
     * @return list of parking lot DTOs
     */
    @GetMapping
    @Operation(
        summary = "Get all parking lots",
        description = "Retrieves a list of all active parking lots. Supports optional page and size query parameters."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved parking lots",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParkingLotResponse.class))),
        headers = {
            @Header(name = "X-Total-Count", description = "Total number of elements across all pages", schema = @Schema(type = "integer")),
            @Header(name = "X-Total-Pages", description = "Total number of pages", schema = @Schema(type = "integer")),
            @Header(name = "X-Current-Page", description = "Current page index", schema = @Schema(type = "integer")),
            @Header(name = "X-Page-Size", description = "Number of elements in current page", schema = @Schema(type = "integer"))
        }
    )
    public ResponseEntity<List<ParkingLotResponse>> getAllParkingLots(
        @Parameter(description = "Zero-based page index")
        @RequestParam(name = "page", required = false) Integer page,
        @Parameter(description = "The size of the page to be returned")
        @RequestParam(name = "size", required = false) Integer size
    ) {
        if (page != null || size != null) {
            int pageIndex = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 20;
            Pageable pageable = PageRequest.of(pageIndex, pageSize);
            Page<ParkingLotResponse> lotPage = parkingLotService.getAllParkingLots(pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(lotPage.getTotalElements()));
            headers.add("X-Total-Pages", String.valueOf(lotPage.getTotalPages()));
            headers.add("X-Current-Page", String.valueOf(lotPage.getNumber()));
            headers.add("X-Page-Size", String.valueOf(lotPage.getSize()));

            return ResponseEntity.ok().headers(headers).body(lotPage.getContent());
        }

        List<ParkingLotResponse> lots = parkingLotService.getAllParkingLots();
        return ResponseEntity.ok(lots);
    }

    /**
     * Retrieves a single parking lot by ID.
     *
     * @param id parking lot ID
     * @return parking lot details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get parking lot by ID", description = "Returns parking lot details for the given ID.")
    @ApiResponse(responseCode = "200", description = "Parking lot found", content = @Content(schema = @Schema(implementation = ParkingLotResponse.class)))
    @ApiResponse(responseCode = "404", description = "Parking lot not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ParkingLotResponse> getParkingLotById(@PathVariable Long id) {
        return ResponseEntity.ok(parkingLotService.getParkingLotById(id));
    }

    /**
     * Creates a new parking lot.
     *
     * @param request creation details
     * @return 201 Created with Location header and created resource
     */
    @PostMapping
    @Operation(summary = "Create parking lot", description = "Creates a new parking lot facility.")
    @ApiResponse(responseCode = "201", description = "Parking lot created successfully", headers = @Header(name = "Location", description = "URI of the created resource"))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Parking lot with name already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ParkingLotResponse> createParkingLot(@Valid @RequestBody CreateParkingLotRequest request) {
        ParkingLotResponse created = parkingLotService.createParkingLot(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing parking lot.
     *
     * @param id parking lot ID
     * @param request update payload
     * @return updated parking lot
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update parking lot", description = "Updates details of an existing parking lot.")
    @ApiResponse(responseCode = "200", description = "Parking lot updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Parking lot not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name conflict with another lot", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ParkingLotResponse> updateParkingLot(
        @PathVariable Long id,
        @Valid @RequestBody UpdateParkingLotRequest request
    ) {
        return ResponseEntity.ok(parkingLotService.updateParkingLot(id, request));
    }

    /**
     * Deletes a parking lot by ID.
     *
     * @param id parking lot ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete parking lot", description = "Deactivates a parking lot by ID.")
    @ApiResponse(responseCode = "204", description = "Parking lot deleted successfully")
    @ApiResponse(responseCode = "404", description = "Parking lot not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteParkingLot(@PathVariable Long id) {
        parkingLotService.deleteParkingLot(id);
        return ResponseEntity.noContent().build();
    }
}
