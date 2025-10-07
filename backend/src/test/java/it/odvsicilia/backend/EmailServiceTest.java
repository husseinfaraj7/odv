package it.odvsicilia.backend;

import it.odvsicilia.backend.service.EmailService;
import it.odvsicilia.backend.exception.EmailDeliveryException;
import it.odvsicilia.backend.exception.EmailInvalidRecipientException;
import it.odvsicilia.backend.exception.EmailSmtpException;
import it.odvsicilia.backend.exception.EmailAuthenticationException;
import it.odvsicilia.backend.exception.EmailApiTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.AddressException;
import jakarta.mail.MessagingException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import java.net.ConnectException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "senderEmail", "test@odvsicilia.it");
        ReflectionTestUtils.setField(emailService, "senderName", "ODV Sicilia Test");
        ReflectionTestUtils.setField(emailService, "adminEmail", "admin@odvsicilia.it");
        
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testExtractTemplateParameters() {
        String htmlContent = "<html><body>Hello {{name}}, your order {{orderNumber}} is ready!</body></html>";
        
        Set<String> parameters = emailService.extractTemplateParameters(htmlContent);
        
        assertEquals(2, parameters.size());
        assertTrue(parameters.contains("name"));
        assertTrue(parameters.contains("orderNumber"));
    }

    @Test
    void testProcessTemplateParameters() {
        String htmlTemplate = "<html><body>Hello {{name}}, your order {{orderNumber}} is ready!</body></html>";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Mario Rossi");
        parameters.put("orderNumber", "ORD-001");
        
        String result = emailService.processTemplateParameters(htmlTemplate, parameters);
        
        assertEquals("<html><body>Hello Mario Rossi, your order ORD-001 is ready!</body></html>", result);
    }

    @Test
    void testProcessTemplateParametersWithNullValue() {
        String htmlTemplate = "<html><body>Hello {{name}}, {{message}}</body></html>";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Mario Rossi");
        parameters.put("message", null);
        
        String result = emailService.processTemplateParameters(htmlTemplate, parameters);
        
        assertEquals("<html><body>Hello Mario Rossi, </body></html>", result);
    }

    @Test
    void testValidateTemplateParametersSuccess() {
        String htmlTemplate = "<html><body>Hello {{name}}</body></html>";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Mario Rossi");
        
        assertDoesNotThrow(() -> emailService.validateTemplateParameters(htmlTemplate, parameters));
    }

    @Test
    void testValidateTemplateParametersMissingParams() {
        String htmlTemplate = "<html><body>Hello {{name}}, your order {{orderNumber}} is ready!</body></html>";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Mario Rossi");
        
        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class, 
            () -> emailService.validateTemplateParameters(htmlTemplate, parameters));
        
        assertTrue(exception.getMessage().contains("orderNumber"));
        assertEquals("TEMPLATE_PARAMS_MISSING", exception.getErrorCode());
    }

    @Test
    void testSendContactNotificationToAdminWithLegacyParameters() {
        assertDoesNotThrow(() -> 
            emailService.sendContactNotificationToAdmin("Mario Rossi", "mario@example.com", "Test Subject", "Test Message", "123456789")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }



    @Test
    void testSendContactConfirmationWithLegacyParameters() {
        assertDoesNotThrow(() -> 
            emailService.sendContactConfirmationToCustomer("Mario Rossi", "mario@example.com", "Test Subject")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendOrderNotificationWithLegacyParameters() {
        assertDoesNotThrow(() -> 
            emailService.sendOrderNotificationToAdmin("ORD-001", "Mario Rossi", "mario@example.com", "100.00", "Item 1 x 2")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendOrderConfirmationWithLegacyParameters() {
        assertDoesNotThrow(() -> 
            emailService.sendOrderConfirmationToCustomer("Mario Rossi", "mario@example.com", "ORD-001", "100.00", "Item 1 x 2")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }



    @Test
    void testValidationFailsWithMissingCustomerName() {
        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class, 
            () -> emailService.sendContactNotificationToAdmin("", "mario@example.com", "Test Subject", "Test Message", "123456789"));
        
        assertTrue(exception.getMessage().contains("Nome cliente"));
    }

    @Test
    void testValidationFailsWithMissingCustomerEmail() {
        EmailInvalidRecipientException exception = assertThrows(EmailInvalidRecipientException.class, 
            () -> emailService.sendContactNotificationToAdmin("Mario Rossi", null, "Test Subject", "Test Message", "123456789"));
        
        assertEquals("RECIPIENT_EMAIL_EMPTY", exception.getErrorCode());
    }

    @Test
    void testValidateRecipientEmailWithValidEmail() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        assertDoesNotThrow(() -> {
            try {
                validateMethod.invoke(emailService, "valid@example.com");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testValidateRecipientEmailWithComplexValidEmail() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        assertDoesNotThrow(() -> {
            try {
                validateMethod.invoke(emailService, "mario.rossi+test@example-domain.com");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testValidateRecipientEmailWithNullEmail() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(emailService, (String) null);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
        
        assertTrue(exception instanceof EmailInvalidRecipientException);
        EmailInvalidRecipientException emailException = (EmailInvalidRecipientException) exception;
        assertEquals("RECIPIENT_EMAIL_EMPTY", emailException.getErrorCode());
        assertTrue(emailException.getMessage().contains("vuota"));
    }

    @Test
    void testValidateRecipientEmailWithEmptyEmail() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(emailService, "");
            } catch (Exception e) {
                throw e.getCause();
            }
        });
        
        assertTrue(exception instanceof EmailInvalidRecipientException);
        EmailInvalidRecipientException emailException = (EmailInvalidRecipientException) exception;
        assertEquals("RECIPIENT_EMAIL_EMPTY", emailException.getErrorCode());
    }

    @Test
    void testValidateRecipientEmailWithWhitespaceEmail() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(emailService, "   ");
            } catch (Exception e) {
                throw e.getCause();
            }
        });
        
        assertTrue(exception instanceof EmailInvalidRecipientException);
        EmailInvalidRecipientException emailException = (EmailInvalidRecipientException) exception;
        assertEquals("RECIPIENT_EMAIL_EMPTY", emailException.getErrorCode());
    }

    @Test
    void testValidateRecipientEmailWithInvalidEmailFormat() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(emailService, "invalid-email");
            } catch (Exception e) {
                throw e.getCause();
            }
        });
        
        assertTrue(exception instanceof EmailInvalidRecipientException);
        EmailInvalidRecipientException emailException = (EmailInvalidRecipientException) exception;
        assertEquals("RECIPIENT_EMAIL_INVALID", emailException.getErrorCode());
        assertTrue(emailException.getCause() instanceof AddressException);
        assertTrue(emailException.getMessage().contains("invalid-email"));
    }

    @Test
    void testValidateRecipientEmailWithInvalidEmailMissingDomain() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(emailService, "user@");
            } catch (Exception e) {
                throw e.getCause();
            }
        });
        
        assertTrue(exception instanceof EmailInvalidRecipientException);
        EmailInvalidRecipientException emailException = (EmailInvalidRecipientException) exception;
        assertEquals("RECIPIENT_EMAIL_INVALID", emailException.getErrorCode());
        assertTrue(emailException.getCause() instanceof AddressException);
    }

    @Test
    void testValidateRecipientEmailWithInvalidEmailMissingAtSymbol() throws Exception {
        Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
        validateMethod.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(emailService, "userexample.com");
            } catch (Exception e) {
                throw e.getCause();
            }
        });
        
        assertTrue(exception instanceof EmailInvalidRecipientException);
        EmailInvalidRecipientException emailException = (EmailInvalidRecipientException) exception;
        assertEquals("RECIPIENT_EMAIL_INVALID", emailException.getErrorCode());
        assertTrue(emailException.getCause() instanceof AddressException);
    }

    @Test
    void testSMTPConnectionSuccess() throws Exception {
        // Test successful SMTP connection and email send
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        // This should complete without throwing exceptions
        assertDoesNotThrow(() -> {
            emailService.sendEmailViaSMTP("test@example.com", "Test User", "Test Subject", "<html><body>Test content</body></html>");
        });
        
        verify(javaMailSender, times(1)).send(mockMessage);
    }

    @Test
    void testSMTPConnectionException() throws Exception {
        // Simulate ConnectException during SMTP send
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        ConnectException connectException = new ConnectException("Connection refused");
        doThrow(connectException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailSmtpException exception = assertThrows(EmailSmtpException.class, () -> {
            emailService.sendEmailViaSMTP("test@example.com", "Test User", "Test Subject", "<html><body>Test content</body></html>");
        });
        
        assertEquals("SMTP_CONNECTION_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Errore connessione server SMTP"));
        assertTrue(exception.getCause() instanceof ConnectException);
    }

    @Test
    void testSMTPAuthenticationException() throws Exception {
        // Simulate AuthenticationFailedException during SMTP send
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        AuthenticationFailedException authException = new AuthenticationFailedException("Authentication failed");
        doThrow(authException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailAuthenticationException exception = assertThrows(EmailAuthenticationException.class, () -> {
            emailService.sendEmailViaSMTP("test@example.com", "Test User", "Test Subject", "<html><body>Test content</body></html>");
        });
        
        assertEquals("SMTP_AUTHENTICATION_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Errore autenticazione SMTP"));
        assertTrue(exception.getCause() instanceof AuthenticationFailedException);
    }

    @Test
    void testSMTPSendFailedException() throws Exception {
        // Simulate SendFailedException during SMTP send
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        SendFailedException sendException = new SendFailedException("Invalid recipient");
        doThrow(sendException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailInvalidRecipientException exception = assertThrows(EmailInvalidRecipientException.class, () -> {
            emailService.sendEmailViaSMTP("invalid@example.com", "Test User", "Test Subject", "<html><body>Test content</body></html>");
        });
        
        assertEquals("SMTP_SEND_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Errore invio SMTP - destinatario non valido"));
        assertTrue(exception.getCause() instanceof SendFailedException);
    }

    @Test
    void testSMTPTimeoutException() throws Exception {
        // Simulate timeout MessagingException during SMTP send
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        MessagingException timeoutException = new MessagingException("Connection timed out");
        doThrow(timeoutException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailApiTimeoutException exception = assertThrows(EmailApiTimeoutException.class, () -> {
            emailService.sendEmailViaSMTP("test@example.com", "Test User", "Test Subject", "<html><body>Test content</body></html>");
        });
        
        assertEquals("SMTP_TIMEOUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Timeout durante invio email SMTP"));
        assertTrue(exception.getCause() instanceof MessagingException);
    }

    @Test
    void testSMTPGeneralMessagingException() throws Exception {
        // Simulate general MessagingException during SMTP send
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        MessagingException messagingException = new MessagingException("General messaging error");
        doThrow(messagingException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailSmtpException exception = assertThrows(EmailSmtpException.class, () -> {
            emailService.sendEmailViaSMTP("test@example.com", "Test User", "Test Subject", "<html><body>Test content</body></html>");
        });
        
        assertEquals("SMTP_MESSAGING_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Errore invio email via SMTP"));
        assertTrue(exception.getCause() instanceof MessagingException);
    }

    @Test
    void testSMTPUnexpectedException() throws Exception {
        // Simulate unexpected runtime exception during SMTP send
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        RuntimeException runtimeException = new RuntimeException("Unexpected error");
        doThrow(runtimeException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailSmtpException exception = assertThrows(EmailSmtpException.class, () -> {
            emailService.sendEmailViaSMTP("test@example.com", "Test User", "Test Subject", "<html><body>Test content</body></html>");
        });
        
        assertEquals("SMTP_UNEXPECTED_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Errore imprevisto invio email SMTP"));
        assertTrue(exception.getCause() instanceof RuntimeException);
    }
    
    @Test
    void testContactNotificationIntegrationWithSMTPErrors() {
        // Test that contact notification properly handles SMTP errors
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        ConnectException connectException = new ConnectException("SMTP server unavailable");
        doThrow(connectException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailSmtpException exception = assertThrows(EmailSmtpException.class, () -> {
            emailService.sendContactNotificationToAdmin("Mario Rossi", "mario@example.com", "Test Subject", "Test Message", "123456789");
        });
        
        assertEquals("SMTP_CONNECTION_FAILED", exception.getErrorCode());
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testOrderNotificationIntegrationWithSMTPErrors() {
        // Test that order notification properly handles SMTP errors
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        
        AuthenticationFailedException authException = new AuthenticationFailedException("Invalid credentials");
        doThrow(authException).when(javaMailSender).send(any(MimeMessage.class));
        
        EmailAuthenticationException exception = assertThrows(EmailAuthenticationException.class, () -> {
            emailService.sendOrderNotificationToAdmin("ORD-001", "Mario Rossi", "mario@example.com", "100.00", "Item 1 x 2");
        });
        
        assertEquals("SMTP_AUTHENTICATION_FAILED", exception.getErrorCode());
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
}