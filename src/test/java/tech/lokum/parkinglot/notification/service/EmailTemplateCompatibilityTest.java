package tech.lokum.parkinglot.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTemplateCompatibilityTest {

    private EmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new EmailTemplateService("support@parkinglotsystem.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"welcome-user", "booking-confirmation", "payment-receipt"})
    @DisplayName("Should load both HTML and TXT files for each email template")
    void shouldLoadTemplatesSuccessfully(String templateName) {
        String html = templateService.loadTemplate(templateName, "html");
        String txt = templateService.loadTemplate(templateName, "txt");

        assertThat(html).isNotBlank();
        assertThat(txt).isNotBlank();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when template does not exist")
    void shouldThrowWhenTemplateDoesNotExist() {
        assertThatThrownBy(() -> templateService.loadTemplate("non-existent-template", "html"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template file not found on classpath");
    }

    @ParameterizedTest
    @ValueSource(strings = {"welcome-user", "booking-confirmation", "payment-receipt"})
    @DisplayName("HTML templates must conform to Outlook and Gmail email client compatibility standards")
    void shouldConformToEmailClientCompatibilityStandards(String templateName) {
        String html = templateService.loadTemplate(templateName, "html");

        // 1. DOCTYPE XHTML 1.0 Transitional for reliable rendering in desktop clients
        assertThat(html).contains("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"");

        // 2. Viewport meta tag for mobile clients (Gmail, Apple Mail)
        assertThat(html).contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />");

        // 3. UTF-8 Content-Type meta
        assertThat(html).contains("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");

        // 4. Microsoft Outlook conditional comments (MSO)
        assertThat(html).contains("<!--[if mso]>");
        assertThat(html).contains("<!--[if (gte mso 9)|(IE)]>");

        // 5. Table-based layout with role=presentation for accessibility and rendering engine fallback
        assertThat(html).contains("<table role=\"presentation\"");

        // 6. Inline styles present
        assertThat(html).contains("style=\"");
    }

    @Test
    @DisplayName("Welcome user template should replace all placeholders without leaving unreplaced tokens")
    void shouldRenderWelcomeUserTemplateWithoutOrphanPlaceholders() {
        Map<String, String> vars = new HashMap<>();
        vars.put("fullName", "John Doe");
        vars.put("username", "johndoe");
        vars.put("email", "john@example.com");
        vars.put("role", "ADMIN");
        vars.put("supportEmail", "help@parkinglot.com");
        vars.put("year", "2026");

        String html = templateService.renderHtml(EmailTemplateService.TEMPLATE_WELCOME, vars);
        String txt = templateService.renderPlainText(EmailTemplateService.TEMPLATE_WELCOME, vars);

        assertThat(html).contains("John Doe")
                .contains("johndoe")
                .contains("john@example.com")
                .contains("ADMIN")
                .contains("help@parkinglot.com")
                .contains("2026");

        assertThat(txt).contains("John Doe")
                .contains("johndoe")
                .contains("john@example.com")
                .contains("ADMIN");

        assertNoRemainingPlaceholders(html);
        assertNoRemainingPlaceholders(txt);
    }

    @Test
    @DisplayName("Booking confirmation template should replace all placeholders cleanly")
    void shouldRenderBookingConfirmationWithoutOrphanPlaceholders() {
        Map<String, String> vars = new HashMap<>();
        vars.put("fullName", "Jane Smith");
        vars.put("reservationId", "882");
        vars.put("status", "CONFIRMED");
        vars.put("lotName", "Central Plaza");
        vars.put("spotNumber", "B-12");
        vars.put("licensePlate", "ABC-1234");
        vars.put("startTime", "2026-10-01 10:00 UTC");
        vars.put("endTime", "2026-10-01 14:00 UTC");
        vars.put("totalAmount", "$24.00");
        vars.put("supportEmail", "support@parkinglot.com");
        vars.put("year", "2026");

        String html = templateService.renderHtml(EmailTemplateService.TEMPLATE_BOOKING, vars);
        String txt = templateService.renderPlainText(EmailTemplateService.TEMPLATE_BOOKING, vars);

        assertThat(html).contains("Jane Smith")
                .contains("#882")
                .contains("Central Plaza")
                .contains("B-12")
                .contains("ABC-1234")
                .contains("$24.00");

        assertThat(txt).contains("Jane Smith")
                .contains("B-12")
                .contains("$24.00");

        assertNoRemainingPlaceholders(html);
        assertNoRemainingPlaceholders(txt);
    }

    @Test
    @DisplayName("Payment receipt template should replace all placeholders cleanly")
    void shouldRenderPaymentReceiptWithoutOrphanPlaceholders() {
        Map<String, String> vars = new HashMap<>();
        vars.put("fullName", "Bruce Wayne");
        vars.put("paymentId", "991");
        vars.put("transactionId", "TX-100293");
        vars.put("reservationId", "554");
        vars.put("amount", "$50.00");
        vars.put("currency", "USD");
        vars.put("paymentMethod", "CREDIT_CARD");
        vars.put("paymentStatus", "SUCCESS");
        vars.put("paidAt", "2026-10-01 12:00 UTC");
        vars.put("supportEmail", "support@parkinglot.com");
        vars.put("year", "2026");

        String html = templateService.renderHtml(EmailTemplateService.TEMPLATE_PAYMENT, vars);
        String txt = templateService.renderPlainText(EmailTemplateService.TEMPLATE_PAYMENT, vars);

        assertThat(html).contains("Bruce Wayne")
                .contains("#991")
                .contains("TX-100293")
                .contains("#554")
                .contains("$50.00 USD")
                .contains("CREDIT_CARD")
                .contains("SUCCESS");

        assertThat(txt).contains("Bruce Wayne")
                .contains("TX-100293")
                .contains("$50.00 USD");

        assertNoRemainingPlaceholders(html);
        assertNoRemainingPlaceholders(txt);
    }

    @Test
    @DisplayName("replacePlaceholders should handle null or empty inputs gracefully")
    void shouldHandleNullOrEmptySafely() {
        assertThat(templateService.replacePlaceholders(null, Map.of())).isEmpty();
        assertThat(templateService.replacePlaceholders("Test {{key}}", null)).isEqualTo("Test {{key}}");
        assertThat(templateService.replacePlaceholders("Test {{key}}", Map.of())).isEqualTo("Test {{key}}");
    }

    private void assertNoRemainingPlaceholders(String content) {
        Pattern pattern = Pattern.compile("\\{\\{[a-zA-Z0-9_]+\\}\\}");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find())
                .withFailMessage(() -> "Found unreplaced placeholder: " + matcher.group())
                .isFalse();
    }
}
