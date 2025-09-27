package it.odvsicilia.backend;

import it.odvsicilia.backend.service.EmailService;
import it.odvsicilia.backend.exception.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
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
    void testSendContactNotificationToAdminWithTemplateParams() {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("customerName", "Mario Rossi");
        templateParams.put("customerEmail", "mario@example.com");
        templateParams.put("subject", "Test Subject");
        templateParams.put("message", "Test Message");
        templateParams.put("phone", "123456789");
        
        assertDoesNotThrow(() -> emailService.sendContactNotificationToAdmin(templateParams));
        
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
    void testSendEmailWithTemplate() {
        String htmlTemplate = "<html><body>Hello {{name}}!</body></html>";
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("name", "Mario Rossi");
        
        assertDoesNotThrow(() -> 
            emailService.sendEmailWithTemplate("mario@example.com", "Mario Rossi", "Test Subject", htmlTemplate, templateParams)
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testValidationFailsWithMissingCustomerName() {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("customerName", "");
        templateParams.put("customerEmail", "mario@example.com");
        templateParams.put("subject", "Test Subject");
        
        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class, 
            () -> emailService.sendContactNotificationToAdmin(templateParams));
        
        assertEquals("CUSTOMER_NAME_REQUIRED", exception.getErrorCode());
    }

    @Test
    void testValidationFailsWithMissingCustomerEmail() {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("customerName", "Mario Rossi");
        templateParams.put("customerEmail", null);
        templateParams.put("subject", "Test Subject");
        
        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class, 
            () -> emailService.sendContactNotificationToAdmin(templateParams));
        
        assertEquals("CUSTOMER_EMAIL_REQUIRED", exception.getErrorCode());
    }
}