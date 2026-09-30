package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.dto.PaymentInitiateRequest;
import tech.lokum.parkinglot.dto.PaymentInitiateResponse;
import tech.lokum.parkinglot.dto.PaymentNotificationRequest;
import tech.lokum.parkinglot.dto.PaymentNotificationResponse;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.dto.PaymentWebhookResponse;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.gateway.PaymentGatewayType;
import tech.lokum.parkinglot.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @DisplayName("GET /api/payments without filters should return 200 OK and all payments list")
    void shouldReturnAllPayments() throws Exception {
        PaymentResponse response = new PaymentResponse(
            301L,
            501L,
            BigDecimal.valueOf(25.00),
            PaymentMethod.CREDIT_CARD,
            "USD",
            PaymentStatus.SUCCESS,
            "STRIPE123456"
        );

        when(paymentService.getAllPayments(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/payments")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].paymentId").value(301))
            .andExpect(jsonPath("$[0].reservationId").value(501))
            .andExpect(jsonPath("$[0].amount").value(25.00))
            .andExpect(jsonPath("$[0].paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$[0].currency").value("USD"))
            .andExpect(jsonPath("$[0].status").value("SUCCESS"))
            .andExpect(jsonPath("$[0].confirmationNumber").value("STRIPE123456"));
    }

    @Test
    @DisplayName("GET /api/payments with status and reservationId filters should filter results")
    void shouldReturnFilteredPayments() throws Exception {
        PaymentResponse response = new PaymentResponse(
            301L,
            501L,
            BigDecimal.valueOf(25.00),
            PaymentMethod.CREDIT_CARD,
            "USD",
            PaymentStatus.SUCCESS,
            "STRIPE123456"
        );

        when(paymentService.getAllPayments(PaymentStatus.SUCCESS, 501L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/payments")
                .param("status", "SUCCESS")
                .param("reservationId", "501")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].paymentId").value(301))
            .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/payments with pagination parameters should return 200 OK with pagination headers")
    void shouldReturnPaginatedPaymentsWithHeaders() throws Exception {
        PaymentResponse response = new PaymentResponse(
            301L,
            501L,
            BigDecimal.valueOf(25.00),
            PaymentMethod.CREDIT_CARD,
            "USD",
            PaymentStatus.SUCCESS,
            "STRIPE123456"
        );

        when(paymentService.getAllPayments(eq(null), eq(null), any()))
            .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/payments")
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "1"))
            .andExpect(header().string("X-Total-Pages", "1"))
            .andExpect(header().string("X-Current-Page", "0"))
            .andExpect(header().string("X-Page-Size", "10"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].paymentId").value(301));
    }

    @Test
    @DisplayName("POST /api/payments/notifications should return 200 OK with success message when payload is valid")
    void shouldHandlePaymentNotificationSuccessfully() throws Exception {
        PaymentNotificationResponse response = PaymentNotificationResponse.success();

        when(paymentService.handlePaymentNotification(any(PaymentNotificationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/payments/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "paymentId": 301,
                        "status": "SUCCESS",
                        "confirmationNumber": "STRIPE123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Payment status updated successfully."));
    }

    @Test
    @DisplayName("POST /api/payments/notifications should return 400 Bad Request when mandatory fields are missing")
    void shouldReturn400WhenNotificationMissingFields() throws Exception {
        mockMvc.perform(post("/api/payments/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "paymentId": null,
                        "status": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors.paymentId").isNotEmpty())
            .andExpect(jsonPath("$.validationErrors.status").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/payments/notifications should return 404 Not Found when payment does not exist")
    void shouldReturn404WhenPaymentNotFoundForNotification() throws Exception {
        when(paymentService.handlePaymentNotification(any(PaymentNotificationRequest.class)))
            .thenThrow(new ResourceNotFoundException("Payment", "id", 999L));

        mockMvc.perform(post("/api/payments/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "paymentId": 999,
                        "status": "SUCCESS",
                        "confirmationNumber": "STRIPE123456"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Payment not found with id: '999'"));
    }

    @Test
    @DisplayName("POST /api/payments/initiate should return 201 Created with client secret")
    void shouldInitiatePaymentSuccessfully() throws Exception {
        PaymentInitiateResponse response = new PaymentInitiateResponse(
            301L,
            501L,
            BigDecimal.valueOf(25.00),
            "USD",
            PaymentStatus.PENDING,
            PaymentMethod.CREDIT_CARD,
            PaymentGatewayType.STRIPE,
            "pi_mock_123",
            "pi_mock_123_secret_xyz",
            "Payment initiated successfully"
        );

        when(paymentService.initiatePayment(any(PaymentInitiateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "reservationId": 501,
                        "amount": 25.00,
                        "currency": "USD",
                        "paymentMethod": "CREDIT_CARD",
                        "gatewayType": "STRIPE"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(301))
            .andExpect(jsonPath("$.reservationId").value(501))
            .andExpect(jsonPath("$.clientSecret").value("pi_mock_123_secret_xyz"))
            .andExpect(jsonPath("$.transactionId").value("pi_mock_123"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/payments/initiate should return 400 Bad Request when reservationId is null")
    void shouldReturn400WhenInitiateMissingReservationId() throws Exception {
        mockMvc.perform(post("/api/payments/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "amount": 25.00
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/verify should return 200 OK with updated payment")
    void shouldVerifyPaymentSuccessfully() throws Exception {
        PaymentResponse response = new PaymentResponse(
            301L,
            501L,
            BigDecimal.valueOf(25.00),
            PaymentMethod.CREDIT_CARD,
            "USD",
            PaymentStatus.SUCCESS,
            "pi_mock_123"
        );

        when(paymentService.verifyPayment(301L)).thenReturn(response);

        mockMvc.perform(post("/api/payments/301/verify")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(301))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/payments/webhook should return 200 OK with webhook processing confirmation")
    void shouldHandleWebhookSuccessfully() throws Exception {
        when(paymentService.handleWebhook(any(), any()))
            .thenReturn(PaymentWebhookResponse.processed("Processed event"));

        mockMvc.perform(post("/api/payments/webhook")
                .header("Stripe-Signature", "sig_header")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment_intent.succeeded\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.status").value("processed"));
    }
}
