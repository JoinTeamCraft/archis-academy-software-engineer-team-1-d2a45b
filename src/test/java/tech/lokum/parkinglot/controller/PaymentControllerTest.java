package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.service.PaymentService;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("GET /api/payments/{paymentId} should return 200 OK with full payment details matching ticket contract")
    void shouldReturn200WithPaymentDetails() throws Exception {
        PaymentResponse response = new PaymentResponse(
            301L,
            501L,
            BigDecimal.valueOf(25.00),
            PaymentMethod.CREDIT_CARD,
            "USD",
            PaymentStatus.SUCCESS,
            "STRIPE123456"
        );

        when(paymentService.getPaymentById(301L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/301")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.paymentId").value(301))
            .andExpect(jsonPath("$.reservationId").value(501))
            .andExpect(jsonPath("$.amount").value(25.00))
            .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.confirmationNumber").value("STRIPE123456"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} should return 404 Not Found when payment does not exist")
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        when(paymentService.getPaymentById(999L))
            .thenThrow(new ResourceNotFoundException("Payment", "id", 999L));

        mockMvc.perform(get("/api/payments/999")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Payment not found with id: '999'"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} should return 400 Bad Request when ID is invalid or negative")
    void shouldReturn400WhenPaymentIdIsInvalid() throws Exception {
        when(paymentService.getPaymentById(-5L))
            .thenThrow(new BadRequestException("Payment ID must be a positive number"));

        mockMvc.perform(get("/api/payments/-5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Payment ID must be a positive number"));
    }
}
