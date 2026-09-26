package tech.lokum.parkinglot.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestErrorController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestErrorController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test/errors")
    static class TestErrorController {

        record SampleRequest(@NotBlank(message = "Field name cannot be blank") String name) {}

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("ParkingSpot", "id", 999L);
        }

        @GetMapping("/conflict")
        public void throwConflict() {
            throw new ConflictException("Spot is already reserved for the selected time window");
        }

        @GetMapping("/bad-request")
        public void throwBadRequest() {
            throw new BadRequestException("Start time must be before end time");
        }

        @GetMapping("/unauthorized")
        public void throwUnauthorized() {
            throw new UnauthorizedException("Invalid or expired token");
        }

        @GetMapping("/forbidden")
        public void throwForbidden() {
            throw new ForbiddenException("You do not have permission to access this resource");
        }

        @PostMapping("/validation")
        public void validateBody(@Valid @RequestBody SampleRequest request) {}
    }

    @Test
    @DisplayName("Should return 404 when ResourceNotFoundException is thrown")
    void shouldReturn404ForResourceNotFound() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("ParkingSpot not found with id: '999'"))
            .andExpect(jsonPath("$.path").value("/test/errors/not-found"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 409 when ConflictException is thrown")
    void shouldReturn409ForConflict() throws Exception {
        mockMvc.perform(get("/test/errors/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Spot is already reserved for the selected time window"))
            .andExpect(jsonPath("$.path").value("/test/errors/conflict"));
    }

    @Test
    @DisplayName("Should return 400 when BadRequestException is thrown")
    void shouldReturn400ForBadRequest() throws Exception {
        mockMvc.perform(get("/test/errors/bad-request"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Start time must be before end time"))
            .andExpect(jsonPath("$.path").value("/test/errors/bad-request"));
    }

    @Test
    @DisplayName("Should return 400 with field errors on MethodArgumentNotValidException")
    void shouldReturn400ForValidationErrors() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.validationErrors.name").value("Field name cannot be blank"));
    }

    @Test
    @DisplayName("Should return 401 when UnauthorizedException is thrown")
    void shouldReturn401ForUnauthorized() throws Exception {
        mockMvc.perform(get("/test/errors/unauthorized"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    @DisplayName("Should return 403 when ForbiddenException is thrown")
    void shouldReturn403ForForbidden() throws Exception {
        mockMvc.perform(get("/test/errors/forbidden"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }
}
