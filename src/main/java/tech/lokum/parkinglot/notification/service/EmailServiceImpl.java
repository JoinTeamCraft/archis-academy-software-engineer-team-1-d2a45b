package tech.lokum.parkinglot.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.notification.model.EmailDetails;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of {@link EmailService} utilizing Spring's JavaMailSender with fallback recording.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailTemplateService emailTemplateService;
    private final String fromAddress;
    private final boolean mailEnabled;

    private final List<EmailDetails> sentEmails = new CopyOnWriteArrayList<>();

    public EmailServiceImpl(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            EmailTemplateService emailTemplateService,
            @Value("${app.mail.from:noreply@parkinglot.lokum.tech}") String fromAddress,
            @Value("${app.mail.enabled:true}") boolean mailEnabled
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailTemplateService = emailTemplateService;
        this.fromAddress = fromAddress;
        this.mailEnabled = mailEnabled;
    }

    @Override
    public void sendEmail(EmailDetails emailDetails) {
        if (emailDetails == null || emailDetails.to() == null || emailDetails.to().isBlank()) {
            log.warn("Cannot send email: recipient address is empty or null");
            return;
        }

        sentEmails.add(emailDetails);
        log.info("Dispatching email to '{}' with subject '{}'", emailDetails.to(), emailDetails.subject());

        if (!mailEnabled) {
            log.info("Email delivery is disabled via configuration (app.mail.enabled=false). Recorded in delivery log.");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("JavaMailSender is not configured. Email recorded successfully for audit.");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                log.info("JavaMailSender returned null MimeMessage");
                return;
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(emailDetails.to().trim());
            helper.setSubject(emailDetails.subject());

            if (emailDetails.htmlBody() != null && !emailDetails.htmlBody().isBlank()) {
                helper.setText(emailDetails.body() != null ? emailDetails.body() : "", emailDetails.htmlBody());
            } else {
                helper.setText(emailDetails.body() != null ? emailDetails.body() : "", false);
            }

            mailSender.send(mimeMessage);
            log.info("Successfully sent email to '{}'", emailDetails.to());
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send email to '{}' via SMTP: {}", emailDetails.to(), ex.getMessage(), ex);
        }
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String text) {
        sendEmail(new EmailDetails(to, subject, text, null));
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody, String textFallback) {
        sendEmail(new EmailDetails(to, subject, textFallback, htmlBody));
    }

    @Override
    public void sendUserRegistrationEmail(User user) {
        if (user == null || user.getEmail() == null) {
            log.warn("User or email is null, skipping registration email");
            return;
        }
        EmailDetails details = emailTemplateService.buildUserRegistrationEmail(user);
        sendEmail(details);
    }

    @Override
    public void sendBookingConfirmationEmail(Reservation reservation) {
        if (reservation == null || reservation.getUser() == null || reservation.getUser().getEmail() == null) {
            log.warn("Reservation or user email is null, skipping booking confirmation email");
            return;
        }
        EmailDetails details = emailTemplateService.buildBookingConfirmationEmail(reservation);
        sendEmail(details);
    }

    @Override
    public void sendPaymentReceiptEmail(Payment payment) {
        if (payment == null) {
            log.warn("Payment is null, skipping payment receipt email");
            return;
        }
        EmailDetails details = emailTemplateService.buildPaymentReceiptEmail(payment);
        sendEmail(details);
    }

    @Override
    public List<EmailDetails> getSentEmails() {
        return Collections.unmodifiableList(sentEmails);
    }

    @Override
    public void clearSentEmails() {
        sentEmails.clear();
    }
}
