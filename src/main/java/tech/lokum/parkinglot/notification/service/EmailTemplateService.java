package tech.lokum.parkinglot.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.notification.model.EmailDetails;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for building consistent, structured plain-text and HTML notification email templates
 * using file-based templates stored under classpath:templates/email/.
 */
@Service
public class EmailTemplateService {

    public static final String TEMPLATE_DIR = "templates/email";
    public static final String TEMPLATE_WELCOME = "welcome-user";
    public static final String TEMPLATE_BOOKING = "booking-confirmation";
    public static final String TEMPLATE_PAYMENT = "payment-receipt";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final String supportEmail;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    @Autowired
    public EmailTemplateService(@Value("${app.mail.support-email:support@parkinglotsystem.com}") String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public EmailTemplateService() {
        this("support@parkinglotsystem.com");
    }

    /**
     * Loads a template from classpath:templates/email/{templateName}.{extension}
     */
    public String loadTemplate(String templateName, String extension) {
        String resourcePath = TEMPLATE_DIR + "/" + templateName + "." + extension;
        return templateCache.computeIfAbsent(resourcePath, path -> {
            try (InputStream is = getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalArgumentException("Template file not found on classpath: " + path);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read template: " + path, e);
            }
        });
    }

    /**
     * Replaces {{variable}} placeholders with values from the given map.
     */
    public String replacePlaceholders(String templateContent, Map<String, String> variables) {
        if (templateContent == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return templateContent;
        }
        String result = templateContent;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String replacement = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, replacement);
        }
        return result;
    }

    /**
     * Renders plain-text template.
     */
    public String renderPlainText(String templateName, Map<String, String> variables) {
        String raw = loadTemplate(templateName, "txt");
        return replacePlaceholders(raw, variables);
    }

    /**
     * Renders HTML template.
     */
    public String renderHtml(String templateName, Map<String, String> variables) {
        String raw = loadTemplate(templateName, "html");
        return replacePlaceholders(raw, variables);
    }

    /**
     * Builds registration confirmation email with account details using welcome templates.
     */
    public EmailDetails buildUserRegistrationEmail(User user) {
        String recipient = user.getEmail();
        String name = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : (user.getUsername() != null ? user.getUsername() : "Valued Member");
        String username = user.getUsername() != null ? user.getUsername() : user.getEmail();
        String role = user.getRole() != null ? user.getRole().name() : "USER";

        Map<String, String> vars = new HashMap<>();
        vars.put("fullName", name);
        vars.put("username", username);
        vars.put("email", recipient);
        vars.put("role", role);
        vars.put("supportEmail", supportEmail);
        vars.put("year", String.valueOf(Year.now().getValue()));

        String subject = "Welcome to Parking Lot System - Registration Confirmed";
        String textBody = renderPlainText(TEMPLATE_WELCOME, vars);
        String htmlBody = renderHtml(TEMPLATE_WELCOME, vars);

        return new EmailDetails(recipient, subject, textBody, htmlBody);
    }

    /**
     * Builds booking confirmation email with reservation and spot details using booking confirmation templates.
     */
    public EmailDetails buildBookingConfirmationEmail(Reservation reservation) {
        User user = reservation.getUser();
        String recipient = user.getEmail();
        String name = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : (user.getUsername() != null ? user.getUsername() : "Valued Customer");

        String lotName = reservation.getParkingSpot() != null && reservation.getParkingSpot().getParkingLot() != null
                ? reservation.getParkingSpot().getParkingLot().getName()
                : "Reserved Lot";
        String spotNumber = reservation.getParkingSpot() != null ? reservation.getParkingSpot().getSpotNumber() : "N/A";
        String licensePlate = reservation.getVehicle() != null ? reservation.getVehicle().getLicensePlate() : "N/A";
        String startStr = reservation.getStartTime() != null ? DATE_FORMATTER.format(reservation.getStartTime()) : "N/A";
        String endStr = reservation.getEndTime() != null ? DATE_FORMATTER.format(reservation.getEndTime()) : "N/A";
        String amountStr = reservation.getTotalAmount() != null ? String.format(Locale.US, "$%.2f", reservation.getTotalAmount()) : "$0.00";
        String status = reservation.getStatus() != null ? reservation.getStatus().name() : "CONFIRMED";

        Map<String, String> vars = new HashMap<>();
        vars.put("fullName", name);
        vars.put("reservationId", reservation.getId() != null ? reservation.getId().toString() : "N/A");
        vars.put("status", status);
        vars.put("lotName", lotName);
        vars.put("spotNumber", spotNumber);
        vars.put("licensePlate", licensePlate);
        vars.put("startTime", startStr);
        vars.put("endTime", endStr);
        vars.put("totalAmount", amountStr);
        vars.put("supportEmail", supportEmail);
        vars.put("year", String.valueOf(Year.now().getValue()));

        String subject = String.format("Booking Confirmed - Reservation #%s", vars.get("reservationId"));
        String textBody = renderPlainText(TEMPLATE_BOOKING, vars);
        String htmlBody = renderHtml(TEMPLATE_BOOKING, vars);

        return new EmailDetails(recipient, subject, textBody, htmlBody);
    }

    /**
     * Builds payment receipt email with transaction and fee details using payment receipt templates.
     */
    public EmailDetails buildPaymentReceiptEmail(Payment payment) {
        Reservation reservation = payment.getReservation();
        String recipient = (reservation != null && reservation.getUser() != null)
                ? reservation.getUser().getEmail()
                : "customer@example.com";
        String name = (reservation != null && reservation.getUser() != null && reservation.getUser().getFullName() != null && !reservation.getUser().getFullName().isBlank())
                ? reservation.getUser().getFullName()
                : "Valued Customer";

        Long reservationId = reservation != null ? reservation.getId() : null;
        String txId = payment.getTransactionId() != null ? payment.getTransactionId() : "N/A";
        String amountStr = payment.getAmount() != null ? String.format(Locale.US, "$%.2f", payment.getAmount()) : "$0.00";
        String currencyStr = payment.getCurrency() != null ? payment.getCurrency() : "USD";
        String method = payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "STANDARD";
        String status = payment.getStatus() != null ? payment.getStatus().name() : "SUCCESS";
        String dateStr = payment.getPaidAt() != null ? DATE_FORMATTER.format(payment.getPaidAt()) : DATE_FORMATTER.format(Instant.now());

        Map<String, String> vars = new HashMap<>();
        vars.put("fullName", name);
        vars.put("paymentId", payment.getId() != null ? payment.getId().toString() : "N/A");
        vars.put("transactionId", txId);
        vars.put("reservationId", reservationId != null ? reservationId.toString() : "N/A");
        vars.put("amount", amountStr);
        vars.put("currency", currencyStr);
        vars.put("paymentMethod", method);
        vars.put("paymentStatus", status);
        vars.put("paidAt", dateStr);
        vars.put("supportEmail", supportEmail);
        vars.put("year", String.valueOf(Year.now().getValue()));

        String subject = String.format("Payment Receipt - Transaction #%s", txId);
        String textBody = renderPlainText(TEMPLATE_PAYMENT, vars);
        String htmlBody = renderHtml(TEMPLATE_PAYMENT, vars);

        return new EmailDetails(recipient, subject, textBody, htmlBody);
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public Map<String, String> getCachedTemplates() {
        return Collections.unmodifiableMap(templateCache);
    }

    private InputStream getResourceAsStream(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            InputStream is = cl.getResourceAsStream(path);
            if (is != null) {
                return is;
            }
        }
        cl = getClass().getClassLoader();
        if (cl != null) {
            InputStream is = cl.getResourceAsStream(path);
            if (is != null) {
                return is;
            }
        }
        return ClassLoader.getSystemResourceAsStream(path);
    }
}
