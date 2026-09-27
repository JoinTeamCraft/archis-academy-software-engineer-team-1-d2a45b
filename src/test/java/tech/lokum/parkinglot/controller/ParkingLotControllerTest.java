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
import tech.lokum.parkinglot.dto.CreateParkingLotRequest;
import tech.lokum.parkinglot.dto.ParkingLotResponse;
import tech.lokum.parkinglot.dto.UpdateParkingLotRequest;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.service.ParkingLotService;

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

@WebMvcTest(ParkingLotController.class)
@Import(GlobalExceptionHandler.class)
class ParkingLotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingLotService parkingLotService;

    @Test
    @DisplayName("GET /api/parking-lots should return 200 OK and JSON array of parking lots")
    void shouldGetAllParkingLots() throws Exception {
        ParkingLotResponse lot1 = new ParkingLotResponse(1L, "Downtown Central", "123 Main St", 150);
        ParkingLotResponse lot2 = new ParkingLotResponse(2L, "Airport Long Term", "Terminal 2", 500);
        when(parkingLotService.getAllParkingLots()).thenReturn(List.of(lot1, lot2));

        mockMvc.perform(get("/api/parking-lots"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Downtown Central"))
            .andExpect(jsonPath("$[0].location").value("123 Main St"))
            .andExpect(jsonPath("$[0].capacity").value(150))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Airport Long Term"))
            .andExpect(jsonPath("$[1].location").value("Terminal 2"))
            .andExpect(jsonPath("$[1].capacity").value(500));
    }

    @Test
    @DisplayName("GET /api/parking-lots should return 200 OK and empty array when no lots exist")
    void shouldReturnEmptyListWhenNoParkingLots() throws Exception {
        when(parkingLotService.getAllParkingLots()).thenReturn(List.of());

        mockMvc.perform(get("/api/parking-lots"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/parking-lots?page=0&size=10 should return 200 OK with pagination headers")
    void shouldGetPaginatedParkingLots() throws Exception {
        ParkingLotResponse lot1 = new ParkingLotResponse(1L, "Downtown Central", "123 Main St", 150);
        when(parkingLotService.getAllParkingLots(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(lot1), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/parking-lots")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "1"))
            .andExpect(header().string("X-Total-Pages", "1"))
            .andExpect(header().string("X-Current-Page", "0"))
            .andExpect(header().string("X-Page-Size", "10"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Downtown Central"));
    }

    @Test
    @DisplayName("GET /api/parking-lots with negative page and size should fallback to default page index 0 and size 20")
    void shouldHandleInvalidPaginationParametersGracefully() throws Exception {
        ParkingLotResponse lot1 = new ParkingLotResponse(1L, "Downtown Central", "123 Main St", 150);
        when(parkingLotService.getAllParkingLots(PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(List.of(lot1), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/parking-lots")
                .param("page", "-1")
                .param("size", "-5"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Current-Page", "0"))
            .andExpect(header().string("X-Page-Size", "20"))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/parking-lots/{id} should return 200 OK when found")
    void shouldGetParkingLotById() throws Exception {
        ParkingLotResponse lot = new ParkingLotResponse(1L, "Downtown Central", "123 Main St", 150);
        when(parkingLotService.getParkingLotById(1L)).thenReturn(lot);

        mockMvc.perform(get("/api/parking-lots/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Downtown Central"))
            .andExpect(jsonPath("$.location").value("123 Main St"))
            .andExpect(jsonPath("$.capacity").value(150));
    }

    @Test
    @DisplayName("GET /api/parking-lots/{id} should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(parkingLotService.getParkingLotById(999L))
            .thenThrow(new ResourceNotFoundException("ParkingLot", "id", 999L));

        mockMvc.perform(get("/api/parking-lots/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("ParkingLot not found with id: '999'"));
    }

    @Test
    @DisplayName("POST /api/parking-lots should return 201 Created with Location header")
    void shouldCreateParkingLot() throws Exception {
        ParkingLotResponse created = new ParkingLotResponse(3L, "West End Garage", "789 West St", 80);
        when(parkingLotService.createParkingLot(any(CreateParkingLotRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/parking-lots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "West End Garage",
                        "location": "789 West St",
                        "capacity": 80
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/parking-lots/3"))
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.name").value("West End Garage"))
            .andExpect(jsonPath("$.capacity").value(80));
    }

    @Test
    @DisplayName("POST /api/parking-lots with invalid data should return 400 Bad Request")
    void shouldReturn400OnValidationFailure() throws Exception {
        mockMvc.perform(post("/api/parking-lots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "",
                        "location": "",
                        "capacity": 0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors.name").isNotEmpty())
            .andExpect(jsonPath("$.validationErrors.location").isNotEmpty())
            .andExpect(jsonPath("$.validationErrors.capacity").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/parking-lots should return 409 Conflict if name already exists")
    void shouldReturn409OnDuplicateName() throws Exception {
        when(parkingLotService.createParkingLot(any(CreateParkingLotRequest.class)))
            .thenThrow(new ConflictException("Parking lot with name 'Downtown Central' already exists"));

        mockMvc.perform(post("/api/parking-lots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Downtown Central",
                        "location": "123 Main St",
                        "capacity": 100
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Parking lot with name 'Downtown Central' already exists"));
    }

    @Test
    @DisplayName("PUT /api/parking-lots/{id} should return 200 OK with updated details")
    void shouldUpdateParkingLot() throws Exception {
        ParkingLotResponse updated = new ParkingLotResponse(1L, "Updated Central", "125 Main St", 160);
        when(parkingLotService.updateParkingLot(eq(1L), any(UpdateParkingLotRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/parking-lots/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Updated Central",
                        "location": "125 Main St",
                        "capacity": 160
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Central"))
            .andExpect(jsonPath("$.capacity").value(160));
    }

    @Test
    @DisplayName("DELETE /api/parking-lots/{id} should return 204 No Content")
    void shouldDeleteParkingLot() throws Exception {
        doNothing().when(parkingLotService).deleteParkingLot(1L);

        mockMvc.perform(delete("/api/parking-lots/1"))
            .andExpect(status().isNoContent());
    }
}
