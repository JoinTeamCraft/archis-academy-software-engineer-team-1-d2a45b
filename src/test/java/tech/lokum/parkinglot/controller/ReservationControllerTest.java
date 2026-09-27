package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.dto.CreateReservationRequest;
import tech.lokum.parkinglot.dto.ReservationResponse;
import tech.lokum.parkinglot.dto.UpdateReservationRequest;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.VehicleType;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.service.ReservationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    @DisplayName("POST /api/reservations should return 201 Created with Location and reservationId")
    void shouldCreateReservationSuccessfully() throws Exception {
        Instant start = Instant.parse("2026-10-01T08:00:00Z");
        Instant end = Instant.parse("2026-10-01T10:00:00Z");

        ReservationResponse created = new ReservationResponse(
            501L,
            201L,
            101L,
            start,
            end,
            BigDecimal.valueOf(20.00),
            ReservationStatus.CONFIRMED
        );

        when(reservationService.createReservation(any(CreateReservationRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "vehicleId": 201,
                        "parkingSpotId": 101,
                        "startTime": "2026-10-01T08:00:00",
                        "endTime": "2026-10-01T10:00:00"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/reservations/501"))
            .andExpect(jsonPath("$.id").value(501))
            .andExpect(jsonPath("$.reservationId").value(501))
            .andExpect(jsonPath("$.vehicleId").value(201))
            .andExpect(jsonPath("$.parkingSpotId").value(101))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.totalAmount").value(20.00));
    }

    @Test
    @DisplayName("POST /api/reservations should return 400 Bad Request on missing fields")
    void shouldReturn400OnMissingFields() throws Exception {
        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "vehicleId": null,
                        "parkingSpotId": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors.vehicleId").isNotEmpty())
            .andExpect(jsonPath("$.validationErrors.parkingSpotId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/reservations should return 404 Not Found when referenced resource is missing")
    void shouldReturn404WhenResourceNotFound() throws Exception {
        when(reservationService.createReservation(any(CreateReservationRequest.class)))
            .thenThrow(new ResourceNotFoundException("ParkingSpot", "id", 999L));

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "vehicleId": 201,
                        "parkingSpotId": 999,
                        "startTime": "2026-10-01T08:00:00Z",
                        "endTime": "2026-10-01T10:00:00Z"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("ParkingSpot not found with id: '999'"));
    }

    @Test
    @DisplayName("POST /api/reservations should return 409 Conflict when spot has overlapping booking")
    void shouldReturn409WhenOverlapping() throws Exception {
        when(reservationService.createReservation(any(CreateReservationRequest.class)))
            .thenThrow(new ConflictException("Parking spot 'A-101' is already reserved for the requested time window"));

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "vehicleId": 201,
                        "parkingSpotId": 101,
                        "startTime": "2026-10-01T08:00:00Z",
                        "endTime": "2026-10-01T10:00:00Z"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Parking spot 'A-101' is already reserved for the requested time window"));
    }

    @Test
    @DisplayName("GET /api/reservations/{id} should return 200 OK when found")
    void shouldGetReservationById() throws Exception {
        ReservationResponse res = new ReservationResponse(
            501L, 201L, 101L,
            Instant.now(), Instant.now().plusSeconds(3600),
            BigDecimal.valueOf(10.00), ReservationStatus.CONFIRMED
        );

        when(reservationService.getReservationById(501L)).thenReturn(res);

        mockMvc.perform(get("/api/reservations/501"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(501))
            .andExpect(jsonPath("$.reservationId").value(501))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/reservations/{id} should return 404 when not found")
    void shouldReturn404WhenReservationNotFound() throws Exception {
        when(reservationService.getReservationById(999L))
            .thenThrow(new ResourceNotFoundException("Reservation", "id", 999L));

        mockMvc.perform(get("/api/reservations/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/reservations should return 200 OK and list of reservations")
    void shouldGetAllReservations() throws Exception {
        ReservationResponse res = new ReservationResponse(
            501L, 201L, 101L,
            Instant.now(), Instant.now().plusSeconds(3600),
            BigDecimal.valueOf(10.00), ReservationStatus.CONFIRMED
        );

        when(reservationService.getAllReservations()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/reservations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].reservationId").value(501));
    }

    @Test
    @DisplayName("PUT /api/reservations/{id} should return 200 OK with updated reservation")
    void shouldUpdateReservation() throws Exception {
        ReservationResponse updated = new ReservationResponse(
            501L, 201L, 101L,
            Instant.now(), Instant.now().plusSeconds(7200),
            BigDecimal.valueOf(20.00), ReservationStatus.CONFIRMED
        );

        when(reservationService.updateReservation(eq(501L), any(UpdateReservationRequest.class)))
            .thenReturn(updated);

        mockMvc.perform(put("/api/reservations/501")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "status": "CONFIRMED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(501))
            .andExpect(jsonPath("$.totalAmount").value(20.00));
    }

    @Test
    @DisplayName("DELETE /api/reservations/{id} should return 204 No Content")
    void shouldDeleteReservation() throws Exception {
        doNothing().when(reservationService).deleteReservation(501L);

        mockMvc.perform(delete("/api/reservations/501"))
            .andExpect(status().isNoContent());
    }
}
