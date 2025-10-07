package it.odvsicilia.backend;

import it.odvsicilia.backend.service.EmailService;
import it.odvsicilia.backend.exception.EmailSmtpException;
import it.odvsicilia.backend.exception.EmailAuthenticationException;
import it.odvsicilia.backend.exception.EmailApiTimeoutException;
import it.odvsicilia.backend.exception.EmailInvalidRecipientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.mail.host=smtp.gmail.com",
    "spring.mail.port=587",
    "spring.mail.username=test@example.com",
    "spring.mail.password=invalid_password",
    "spring.mail.properties.mail.smtp.auth=true",
    "spring.mail.properties.mail.smtp.starttls.enable=true",
    "brevo.sender.email=test@odvsicilia.it",
    "brevo.sender.name=ODV Sicilia Test",
    "brevo.admin.email=admin@odvsicilia.it"
})
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Ensure all required properties are set for testing
        ReflectionTestUtils.setField(emailService, "senderEmail", "test@odvsicilia.it");
        ReflectionTestUtils.setField(emailService, "senderName", "ODV Sicilia Test");
        ReflectionTestUtils.setField(emailService, "adminEmail", "admin@odvsicilia.it");
    }

    @Test
    void testEmailServiceCompilationAndInstantiation() {
        // Verify that EmailService can be instantiated without ConnectException
        assertNotNull(emailService);
        assertDoesNotThrow(() -> {
            // Test that the service methods are available and can be called
            emailService.getClass().getMethod("sendEmailViaSMTP", String.class, String.class, String.class, String.class);
            emailService.getClass().getMethod("sendContactNotificationToAdmin", String.class, String.class, String.class, String.class, String.class);
            emailService.getClass().getMethod("sendContactConfirmationToCustomer", String.class, String.class, String.class);
            emailService.getClass().getMethod("sendOrderNotificationToAdmin", String.class, String.class, String.class, String.class, String.class);
            emailService.getClass().getMethod("sendOrderConfirmationToCustomer", String.class, String.class, String.class, String.class, String.class);
        });
    }

    @Test
    void testSMTPConnectionWithInvalidCredentials() {
        // Test that authentication failures are properly caught and handled
        EmailAuthenticationException exception = assertThrows(EmailAuthenticationException.class, () -> {
            emailService.sendEmailViaSMTP("recipient@example.com", "Test Recipient", 
                "Test Subject", "<html><body>Test email content</body></html>");
        });
        
        assertEquals("SMTP_AUTHENTICATION_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Errore autenticazione SMTP"));
        assertNotNull(exception.getCause());
    }

    @Test
    void testSMTPConnectionWithInvalidHost() {
        // Test with invalid SMTP host to trigger connection errors
        EmailService testService = new EmailService();
        ReflectionTestUtils.setField(testService, "senderEmail", "test@invalid.com");
        ReflectionTestUtils.setField(testService, "senderName", "Test Sender");
        
        // This would normally require a custom JavaMailSender configuration
        // For this test, we'll verify the service handles connection exceptions properly
        assertThrows(EmailSmtpException.class, () -> {
            // The actual connection will fail, which is what we want to test
            testService.sendEmailViaSMTP("recipient@example.com", "Test Recipient", 
                "Test Subject", "<html><body>Test email content</body></html>");
        });
    }

    @Test
    void testContactNotificationWithConnectionErrors() {
        // Test that contact notifications properly handle SMTP connection errors
        Exception exception = assertThrows(Exception.class, () -> {
            emailService.sendContactNotificationToAdmin("Mario Rossi", "mario@example.com", 
                "Test Contact", "This is a test message", "123456789");
        });
        
        // Should be either an authentication error or SMTP error
        assertTrue(exception instanceof EmailAuthenticationException || 
                   exception instanceof EmailSmtpException);
    }

    @Test
    void testOrderNotificationWithConnectionErrors() {
        // Test that order notifications properly handle SMTP connection errors  
        Exception exception = assertThrows(Exception.class, () -> {
            emailService.sendOrderNotificationToAdmin("ORD-TEST-001", "Mario Rossi", 
                "mario@example.com", "99.99", "Test Product x 1");
        });
        
        // Should be either an authentication error or SMTP error
        assertTrue(exception instanceof EmailAuthenticationException || 
                   exception instanceof EmailSmtpException);
    }

    @Test
    void testInvalidRecipientEmailHandling() {
        // Test that invalid recipient emails are properly caught
        EmailInvalidRecipientException exception = assertThrows(EmailInvalidRecipientException.class, () -> {
            emailService.sendContactNotificationToAdmin("Mario Rossi", "invalid-email", 
                "Test Subject", "Test Message", "123456789");
        });
        
        assertEquals("RECIPIENT_EMAIL_INVALID", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("invalid-email"));
    }

    @Test
    void testEmptyRecipientEmailHandling() {
        // Test that empty recipient emails are properly caught
        EmailInvalidRecipientException exception = assertThrows(EmailInvalidRecipientException.class, () -> {
            emailService.sendContactConfirmationToCustomer("Mario Rossi", "", "Test Subject");
        });
        
        assertEquals("RECIPIENT_EMAIL_EMPTY", exception.getErrorCode());
    }

    @Test 
    void testNullRecipientEmailHandling() {
        // Test that null recipient emails are properly caught
        EmailInvalidRecipientException exception = assertThrows(EmailInvalidRecipientException.class, () -> {
            emailService.sendOrderConfirmationToCustomer("Mario Rossi", null, "ORD-001", "50.00", "Item x 1");
        });
        
        assertEquals("RECIPIENT_EMAIL_EMPTY", exception.getErrorCode());
    }

    @Test
    void testTemplateParameterExtraction() {
        // Test template parameter functionality compiles and works
        String templateHtml = "<html><body>Hello {{name}}, your order {{orderNumber}} totals {{amount}}</body></html>";
        
        var parameters = emailService.extractTemplateParameters(templateHtml);
        
        assertEquals(3, parameters.size());
        assertTrue(parameters.contains("name"));
        assertTrue(parameters.contains("orderNumber"));  
        assertTrue(parameters.contains("amount"));
    }

    @Test
    void testTemplateParameterProcessing() {
        // Test template processing functionality
        String template = "<p>Dear {{customerName}}, thank you for order {{orderNumber}}</p>";
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("customerName", "Mario Rossi");
        params.put("orderNumber", "ORD-12345");
        
        String result = emailService.processTemplateParameters(template, params);
        
        assertEquals("<p>Dear Mario Rossi, thank you for order ORD-12345</p>", result);
    }

    @Test
    void testEmailServiceBeanConfiguration() {
        // Verify the EmailService is properly configured as a Spring bean
        assertNotNull(emailService);
        assertTrue(emailService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
    }

    @Test
    void testExceptionHierarchy() {
        // Test that all custom exceptions extend the base EmailDeliveryException
        assertTrue(EmailAuthenticationException.class.getSuperclass() == it.odvsicilia.backend.exception.EmailDeliveryException.class);
        assertTrue(EmailSmtpException.class.getSuperclass() == it.odvsicilia.backend.exception.EmailDeliveryException.class);
        assertTrue(EmailApiTimeoutException.class.getSuperclass() == it.odvsicilia.backend.exception.EmailDeliveryException.class);
        assertTrue(EmailInvalidRecipientException.class.getSuperclass() == it.odvsicilia.backend.exception.EmailDeliveryException.class);
    }
}