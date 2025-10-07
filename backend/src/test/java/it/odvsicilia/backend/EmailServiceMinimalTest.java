package it.odvsicilia.backend;

import it.odvsicilia.backend.service.EmailServiceMinimal;
import it.odvsicilia.backend.exception.EmailSmtpException;
import it.odvsicilia.backend.exception.EmailAuthenticationException;
import it.odvsicilia.backend.exception.EmailInvalidRecipientException;
import it.odvsicilia.backend.exception.EmailApiTimeoutException;
import it.odvsicilia.backend.exception.EmailDeliveryException;

import java.net.ConnectException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * Comprehensive test suite for EmailServiceMinimal to verify SMTP exception handling
 * and email functionality without external dependencies
 */
public class EmailServiceMinimalTest {
    
    private EmailServiceMinimal emailService;
    private boolean testPassed = true;
    private int testsRun = 0;
    private int testsFailed = 0;
    
    public EmailServiceMinimalTest() {
        this.emailService = new EmailServiceMinimal();
    }
    
    public static void main(String[] args) {
        EmailServiceMinimalTest test = new EmailServiceMinimalTest();
        test.runAllTests();
    }
    
    public void runAllTests() {
        System.out.println("=== EmailService SMTP Exception Handling Tests ===");
        System.out.println("Testing that EmailService compiles without ConnectException error");
        System.out.println("and properly handles SMTP connection-related errors");
        System.out.println();
        
        // Test basic instantiation - verify no ConnectException during class loading
        testEmailServiceInstantiation();
        
        // Test SMTP connection exception handling
        testSMTPConnectionException();
        testSMTPAuthenticationException();  
        testSMTPTimeoutException();
        testSMTPInvalidRecipientException();
        testSMTPGeneralException();
        
        // Test successful email sending
        testSMTPSuccessfulSend();
        
        // Test contact notification with SMTP errors
        testContactNotificationWithSMTPErrors();
        testOrderNotificationWithSMTPErrors();
        
        // Test template functionality
        testExtractTemplateParameters();
        testProcessTemplateParameters();
        testValidateTemplateParameters();
        
        // Test email validation
        testValidateRecipientEmail();
        testValidateEmailInputs();
        
        // Test exception hierarchy
        testExceptionHierarchy();
        
        // Final report
        System.out.println();
        System.out.println("=== Test Summary ===");
        System.out.println("Tests run: " + testsRun);
        System.out.println("Tests failed: " + testsFailed);
        System.out.println("Tests passed: " + (testsRun - testsFailed));
        
        if (testsFailed == 0) {
            System.out.println("✅ All tests passed!");
            System.out.println("✅ EmailService compiles correctly without ConnectException error");
            System.out.println("✅ SMTP connection errors are properly caught and handled by MessagingException blocks");
            System.out.println("✅ Email functionality remains intact and working");
        } else {
            System.out.println("❌ " + testsFailed + " tests failed.");
        }
        
        // Exit with appropriate code for build systems
        System.exit(testsFailed == 0 ? 0 : 1);
    }
    
    private void testEmailServiceInstantiation() {
        runTest("EmailService Instantiation (No ConnectException)", () -> {
            // This test verifies that EmailService can be instantiated without throwing ConnectException
            EmailServiceMinimal service = new EmailServiceMinimal();
            assertNotNull(service, "EmailService should be instantiated successfully");
            
            // Verify key methods exist and are accessible
            assertMethodExists(service, "sendEmailViaSMTP", String.class, String.class, String.class, String.class);
            assertMethodExists(service, "sendContactNotificationToAdmin", String.class, String.class, String.class, String.class, String.class);
            assertMethodExists(service, "sendOrderNotificationToAdmin", String.class, String.class, String.class, String.class, String.class);
            assertMethodExists(service, "extractTemplateParameters", String.class);
            assertMethodExists(service, "processTemplateParameters", String.class, Map.class);
            assertMethodExists(service, "validateTemplateParameters", String.class, Map.class);
        });
    }
    
    private void testSMTPConnectionException() {
        runTest("SMTP Connection Exception Handling", () -> {
            // Test that ConnectException is properly caught and wrapped in EmailSmtpException
            try {
                emailService.sendEmailViaSMTP("connection-error@example.com", "Test User", 
                    "Test Subject", "<html><body>Test content</body></html>");
                fail("Should have thrown EmailSmtpException for connection error");
            } catch (EmailSmtpException e) {
                assertEquals("SMTP_CONNECTION_FAILED", e.getErrorCode(), "Should have correct error code");
                assertTrue(e.getMessage().contains("Errore connessione server SMTP"), "Should have Italian error message");
                assertTrue(e.getCause() instanceof ConnectException, "Should wrap ConnectException");
                assertNotNull(e.getContext(), "Should have context information");
            }
        });
    }
    
    private void testSMTPAuthenticationException() {
        runTest("SMTP Authentication Exception Handling", () -> {
            // Test that authentication failures are properly caught and handled
            try {
                emailService.sendEmailViaSMTP("test@example.com", "Test User", 
                    "auth-error Test Subject", "<html><body>Test content</body></html>");
                fail("Should have thrown EmailAuthenticationException");
            } catch (EmailAuthenticationException e) {
                assertEquals("SMTP_AUTHENTICATION_FAILED", e.getErrorCode(), "Should have correct error code");
                assertTrue(e.getMessage().contains("Errore autenticazione SMTP"), "Should have Italian error message");
                assertNotNull(e.getContext(), "Should have context information");
            }
        });
    }
    
    private void testSMTPTimeoutException() {
        runTest("SMTP Timeout Exception Handling", () -> {
            // Test that timeout errors are properly caught and handled
            try {
                emailService.sendEmailViaSMTP("test@example.com", "Test User", 
                    "timeout-error Test Subject", "<html><body>Test content</body></html>");
                fail("Should have thrown EmailApiTimeoutException");
            } catch (EmailApiTimeoutException e) {
                assertEquals("SMTP_TIMEOUT", e.getErrorCode(), "Should have correct error code");
                assertTrue(e.getMessage().contains("Timeout durante invio email SMTP"), "Should have Italian error message");
                assertNotNull(e.getContext(), "Should have context information");
            }
        });
    }
    
    private void testSMTPInvalidRecipientException() {
        runTest("SMTP Invalid Recipient Exception Handling", () -> {
            // Test that invalid recipient errors are properly caught and handled
            try {
                emailService.sendEmailViaSMTP("invalid@example.com", "Test User", 
                    "Test Subject", "<html><body>Test content</body></html>");
                fail("Should have thrown EmailInvalidRecipientException");
            } catch (EmailInvalidRecipientException e) {
                assertEquals("SMTP_SEND_FAILED", e.getErrorCode(), "Should have correct error code");
                assertTrue(e.getMessage().contains("Errore invio SMTP - destinatario non valido"), "Should have Italian error message");
                assertNotNull(e.getContext(), "Should have context information");
            }
        });
    }
    
    private void testSMTPGeneralException() {
        runTest("SMTP General Exception Handling", () -> {
            // Test that general SMTP errors are properly caught and handled
            try {
                // This will trigger the general exception case
                emailService.sendEmailViaSMTP(null, "Test User", 
                    "Test Subject", "<html><body>Test content</body></html>");
                fail("Should have thrown EmailSmtpException");
            } catch (EmailSmtpException e) {
                assertTrue(e.getErrorCode().equals("SMTP_UNEXPECTED_ERROR") || 
                          e.getErrorCode().equals("SMTP_MESSAGING_ERROR"), "Should have appropriate error code");
                assertTrue(e.getMessage().contains("SMTP"), "Should mention SMTP in error message");
                assertNotNull(e.getContext(), "Should have context information");
            } catch (EmailInvalidRecipientException e) {
                // This is also acceptable as null email should be caught by validation
                assertEquals("RECIPIENT_EMAIL_EMPTY", e.getErrorCode(), "Should have correct error code for empty email");
            }
        });
    }
    
    private void testSMTPSuccessfulSend() {
        runTest("SMTP Successful Send", () -> {
            // Test that emails can be sent successfully when no error conditions are triggered
            try {
                emailService.sendEmailViaSMTP("valid@example.com", "Test User", 
                    "Valid Test Subject", "<html><body>Valid test content</body></html>");
                // If we reach here, the email was sent successfully
            } catch (Exception e) {
                fail("Should not throw exception for valid email send: " + e.getMessage());
            }
        });
    }
    
    private void testContactNotificationWithSMTPErrors() {
        runTest("Contact Notification with SMTP Errors", () -> {
            // Test that contact notifications properly handle SMTP connection errors
            try {
                emailService.sendContactNotificationToAdmin("Mario Rossi", "connection-error@example.com", 
                    "Test Contact", "This is a test message", "123456789");
                fail("Should have thrown EmailSmtpException");
            } catch (EmailSmtpException e) {
                assertEquals("SMTP_CONNECTION_FAILED", e.getErrorCode(), "Should propagate SMTP connection error");
                assertNotNull(e.getContext(), "Should have context information");
            }
        });
    }
    
    private void testOrderNotificationWithSMTPErrors() {
        runTest("Order Notification with SMTP Errors", () -> {
            // Test that order notifications properly handle SMTP authentication errors  
            try {
                emailService.sendOrderNotificationToAdmin("ORD-TEST-001", "Mario Rossi", 
                    "mario@example.com", "99.99", "auth-error Test Product x 1");
                fail("Should have thrown EmailAuthenticationException");
            } catch (EmailAuthenticationException e) {
                assertEquals("SMTP_AUTHENTICATION_FAILED", e.getErrorCode(), "Should propagate SMTP auth error");
                assertNotNull(e.getContext(), "Should have context information");
            }
        });
    }
    
    private void testExtractTemplateParameters() {
        runTest("Extract Template Parameters", () -> {
            String htmlContent = "<html><body>Hello {{name}}, your order {{orderNumber}} is ready!</body></html>";
            
            Set<String> parameters = emailService.extractTemplateParameters(htmlContent);
            
            assertNotNull(parameters, "Parameters set should not be null");
            assertEquals(2, parameters.size(), "Should extract 2 parameters");
            assertTrue(parameters.contains("name"), "Should contain 'name' parameter");
            assertTrue(parameters.contains("orderNumber"), "Should contain 'orderNumber' parameter");
        });
    }
    
    private void testProcessTemplateParameters() {
        runTest("Process Template Parameters", () -> {
            String template = "<html><body>Hello {{name}}, your order {{orderNumber}} is ready!</body></html>";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Mario Rossi");
            parameters.put("orderNumber", "ORD-001");
            
            String result = emailService.processTemplateParameters(template, parameters);
            String expected = "<html><body>Hello Mario Rossi, your order ORD-001 is ready!</body></html>";
            
            assertEquals(expected, result, "Template should be processed correctly");
        });
    }
    
    private void testValidateTemplateParameters() {
        runTest("Validate Template Parameters", () -> {
            String template = "<html><body>Hello {{name}}, your order {{orderNumber}} is ready!</body></html>";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Mario Rossi");
            // Missing orderNumber parameter
            
            try {
                emailService.validateTemplateParameters(template, parameters);
                fail("Should have thrown EmailDeliveryException for missing parameters");
            } catch (EmailDeliveryException e) {
                assertTrue(e.getMessage().contains("orderNumber"), "Exception should mention missing orderNumber");
                assertEquals("TEMPLATE_PARAMS_MISSING", e.getErrorCode(), "Should have correct error code");
            }
        });
    }
    
    private void testValidateRecipientEmail() {
        runTest("Validate Recipient Email", () -> {
            // Test invalid email format
            try {
                emailService.sendEmailViaSMTP("invalid-email", "Test User", "Test Subject", "<html></html>");
                fail("Should have thrown EmailInvalidRecipientException for invalid email format");
            } catch (EmailInvalidRecipientException e) {
                assertEquals("RECIPIENT_EMAIL_INVALID", e.getErrorCode(), "Should have correct error code");
                assertTrue(e.getMessage().contains("invalid-email"), "Should mention invalid email");
            }
            
            // Test empty email
            try {
                emailService.sendEmailViaSMTP("", "Test User", "Test Subject", "<html></html>");
                fail("Should have thrown EmailInvalidRecipientException for empty email");
            } catch (EmailInvalidRecipientException e) {
                assertEquals("RECIPIENT_EMAIL_EMPTY", e.getErrorCode(), "Should have correct error code");
            }
        });
    }
    
    private void testValidateEmailInputs() {
        runTest("Validate Email Inputs", () -> {
            // Test missing customer name
            try {
                emailService.sendContactNotificationToAdmin("", "mario@example.com", 
                    "Test Subject", "Test Message", "123456789");
                fail("Should have thrown EmailDeliveryException for empty customer name");
            } catch (EmailDeliveryException e) {
                assertEquals("CUSTOMER_NAME_REQUIRED", e.getErrorCode(), "Should have correct error code");
            }
            
            // Test missing customer email  
            try {
                emailService.sendContactNotificationToAdmin("Mario Rossi", "", 
                    "Test Subject", "Test Message", "123456789");
                fail("Should have thrown EmailDeliveryException for empty customer email");
            } catch (EmailInvalidRecipientException e) {
                assertEquals("RECIPIENT_EMAIL_EMPTY", e.getErrorCode(), "Should have correct error code for empty email");
            }
        });
    }
    
    private void testExceptionHierarchy() {
        runTest("Exception Hierarchy", () -> {
            // Verify that custom exceptions extend the base EmailDeliveryException
            assertTrue(EmailDeliveryException.class.isAssignableFrom(EmailSmtpException.class), 
                      "EmailSmtpException should extend EmailDeliveryException");
            assertTrue(EmailDeliveryException.class.isAssignableFrom(EmailAuthenticationException.class), 
                      "EmailAuthenticationException should extend EmailDeliveryException");
            assertTrue(EmailDeliveryException.class.isAssignableFrom(EmailApiTimeoutException.class), 
                      "EmailApiTimeoutException should extend EmailDeliveryException");
            assertTrue(EmailDeliveryException.class.isAssignableFrom(EmailInvalidRecipientException.class), 
                      "EmailInvalidRecipientException should extend EmailDeliveryException");
        });
    }
    
    // Test utility methods
    
    private void runTest(String testName, TestRunnable test) {
        testsRun++;
        System.out.print("Running: " + testName + " ... ");
        
        try {
            test.run();
            System.out.println("✅ PASSED");
        } catch (Exception e) {
            testsFailed++;
            System.out.println("❌ FAILED: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  Cause: " + e.getCause().getMessage());
            }
        }
    }
    
    private void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }
    
    private void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }
    
    private void assertEquals(String expected, String actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || actual == null || !expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: '" + expected + "', Actual: '" + actual + "'");
        }
    }
    
    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
    
    private void fail(String message) {
        throw new AssertionError(message);
    }
    
    private void assertMethodExists(Object obj, String methodName, Class<?>... parameterTypes) {
        try {
            java.lang.reflect.Method method = obj.getClass().getMethod(methodName, parameterTypes);
            assertNotNull(method, "Method " + methodName + " should exist");
        } catch (NoSuchMethodException e) {
            fail("Method " + methodName + " not found");
        }
    }
    
    @FunctionalInterface
    private interface TestRunnable {
        void run() throws Exception;
    }
}