package tech.lokum.parkinglot.notification.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.notification.model.EmailDetails;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    private EmailTemplateService emailTemplateService;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService();
        lenient().when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        emailService = new EmailServiceImpl(mailSenderProvider, emailTemplateService, "test@parkinglot.tech", true);
    }

    @Test
    @DisplayName("sendEmail should construct and send MimeMessage when JavaMailSender is available")
    void shouldSendMimeMessageSuccessfully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailDetails details = new EmailDetails("target@example.com", "Test Subject", "Test Body", "<h1>Test</h1>");
        emailService.sendEmail(details);

        verify(javaMailSender).send(mimeMessage);
        assertThat(emailService.getSentEmails()).hasSize(1);
        assertThat(emailService.getSentEmails().get(0).to()).isEqualTo("target@example.com");
    }

    @Test
    @DisplayName("sendEmail should gracefully catch and log MailException without throwing")
    void shouldHandleMailExceptionGracefully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(javaMailSender).send(any(MimeMessage.class));

        EmailDetails details = new EmailDetails("target@example.com", "Failing Subject", "Failing Body");

        // Must not throw exception
        emailService.sendEmail(details);

        assertThat(emailService.getSentEmails()).hasSize(1);
    }

    @Test
    @DisplayName("sendEmail should not invoke JavaMailSender when mail is disabled")
    void shouldNotSendWhenDisabled() {
        EmailServiceImpl disabledService = new EmailServiceImpl(mailSenderProvider, emailTemplateService, "test@parkinglot.tech", false);

        EmailDetails details = new EmailDetails("target@example.com", "Subject", "Body");
        disabledService.sendEmail(details);

        verify(javaMailSender, never()).send(any(MimeMessage.class));
        assertThat(disabledService.getSentEmails()).hasSize(1);
    }

    @Test
    @DisplayName("sendUserRegistrationEmail should dispatch formatted welcome email")
    void shouldSendUserRegistrationEmail() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        User user = new User("clark_k", "clark@example.com", "hash", "Clark Kent", null, Role.USER);
        user.setId(50L);

        emailService.sendUserRegistrationEmail(user);

        verify(javaMailSender).send(mimeMessage);
        assertThat(emailService.getSentEmails()).hasSize(1);
        assertThat(emailService.getSentEmails().get(0).to()).isEqualTo("clark@example.com");
        assertThat(emailService.getSentEmails().get(0).subject()).contains("Welcome to Parking Lot System");
    }

    @Test
    @DisplayName("clearSentEmails should clear the in-memory sent list")
    void shouldClearSentEmails() {
        emailService.sendSimpleEmail("user1@example.com", "Sub 1", "Body 1");
        assertThat(emailService.getSentEmails()).isNotEmpty();

        emailService.clearSentEmails();
        assertThat(emailService.getSentEmails()).isEmpty();
    }
}
