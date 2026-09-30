package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Controller providing system status and heartbeat information.
 */
@RestController
@RequestMapping("/api/v1/status")
@Tag(name = "System Status", description = "Endpoints for monitoring application availability and system status")
public class ApiStatusController {

    @GetMapping
    @Operation(
            summary = "Check API status",
            description = "Returns the operational status, current UTC timestamp, and service version for monitoring probes."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API is up and operating normally",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Healthy Status",
                                    value = "{\"status\": \"UP\", \"service\": \"parking-lot-system\", \"version\": \"1.0.0\", \"timestamp\": \"2026-09-30T08:00:00Z\"}"
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "parking-lot-system",
            "version", "1.0.0",
            "timestamp", Instant.now().toString()
        ));
    }
}
