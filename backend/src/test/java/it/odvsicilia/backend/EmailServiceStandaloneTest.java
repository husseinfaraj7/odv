package it.odvsicilia.backend;

import it.odvsicilia.backend.service.EmailService;
import it.odvsicilia.backend.exception.EmailSmtpException;
import it.odvsicilia.backend.exception.EmailAuthenticationException;
import it.odvsicilia.backend.exception.EmailInvalidRecipientException;
import it.odvsicilia.backend.exception.EmailApiTimeoutException;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * Standalone test to verify EmailService compilation and basic functionality
 * without external dependencies like JUnit/Mockito/Spring
 */
public class EmailServiceStandaloneTest {
    
    private EmailService emailService;
    private boolean testPassed = true;
    private int testsRun = 0;
    private int testsFailed = 0;
    
    public EmailServiceStandaloneTest() {
        this.emailService = new EmailService();
        setPrivateField("senderEmail", "test@odvsicilia.it");
        setPrivateField("senderName", "ODV Sicilia Test");
        setPrivateField("adminEmail", "admin@odvsicilia.it");
    }
    
    public static void main(String[] args) {
        EmailServiceStandaloneTest test = new EmailServiceStandaloneTest();
        test.runAllTests();
    }
    
    public void runAllTests() {
        System.out.println("=== EmailService Standalone Tests ===");
        System.out.println();
        
        // Test basic instantiation - verify no ConnectException during class loading
        testEmailServiceInstantiation();
        
        // Test template parameter functionality
        testExtractTemplateParameters();
        testProcessTemplateParameters();
        testValidateTemplateParametersSuccess();
        testValidateTemplateParametersMissing();
        
        // Test validation methods
        testValidateEmailInputsSuccess();
        testValidateEmailInputsFailure();
        testValidateRecipientEmailValid();
        testValidateRecipientEmailInvalid();
        
        // Test exception handling structure
        testExceptionHierarchy();
        
        // Final report
        System.out.println();
        System.out.println("=== Test Summary ===");
        System.out.println("Tests run: " + testsRun);
        System.out.println("Tests failed: " + testsFailed);
        System.out.println("Tests passed: " + (testsRun - testsFailed));
        
        if (testsFailed == 0) {
            System.out.println("✅ All tests passed! EmailService compiles correctly and handles exceptions properly.");
        } else {
            System.out.println("❌ " + testsFailed + " tests failed.");
        }
    }
    
    private void testEmailServiceInstantiation() {
        runTest("EmailService Instantiation", () -> {
            // This test verifies that EmailService can be instantiated without throwing ConnectException
            EmailService service = new EmailService();
            assertNotNull(service, "EmailService should be instantiated successfully");
            
            // Verify key methods exist and are accessible
            assertMethodExists(service, "sendEmailViaSMTP", String.class, String.class, String.class, String.class);
            assertMethodExists(service, "extractTemplateParameters", String.class);
            assertMethodExists(service, "processTemplateParameters", String.class, Map.class);
            assertMethodExists(service, "validateTemplateParameters", String.class, Map.class);
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
    
    private void testValidateTemplateParametersSuccess() {
        runTest("Validate Template Parameters Success", () -> {
            String template = "<html><body>Hello {{name}}</body></html>";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Mario Rossi");
            
            // Should not throw any exception
            emailService.validateTemplateParameters(template, parameters);
        });
    }
    
    private void testValidateTemplateParametersMissing() {
        runTest("Validate Template Parameters Missing", () -> {
            String template = "<html><body>Hello {{name}}, your order {{orderNumber}} is ready!</body></html>";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Mario Rossi");
            // Missing orderNumber parameter
            
            try {
                emailService.validateTemplateParameters(template, parameters);
                fail("Should have thrown EmailDeliveryException for missing parameters");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("orderNumber"), "Exception should mention missing orderNumber");
                assertEquals("TEMPLATE_PARAMS_MISSING", getErrorCode(e), "Should have correct error code");
            }
        });
    }
    
    private void testValidateEmailInputsSuccess() {
        runTest("Validate Email Inputs Success", () -> {
            try {
                Method validateMethod = EmailService.class.getDeclaredMethod("validateEmailInputs", String.class, String.class, String.class);
                validateMethod.setAccessible(true);
                
                // Should not throw exception
                validateMethod.invoke(emailService, "Mario Rossi", "mario@example.com", "Test Subject");
            } catch (Exception e) {
                if (e.getCause() != null) {
                    throw new RuntimeException(e.getCause());
                }
                throw new RuntimeException(e);
            }
        });
    }
    
    private void testValidateEmailInputsFailure() {
        runTest("Validate Email Inputs Failure", () -> {
            try {
                Method validateMethod = EmailService.class.getDeclaredMethod("validateEmailInputs", String.class, String.class, String.class);
                validateMethod.setAccessible(true);
                
                try {
                    validateMethod.invoke(emailService, "", "mario@example.com", "Test Subject");
                    fail("Should have thrown exception for empty customer name");
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    assertNotNull(cause, "Should have thrown an exception");
                    assertTrue(cause.getMessage().contains("Nome cliente"), "Should mention customer name requirement");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private void testValidateRecipientEmailValid() {
        runTest("Validate Recipient Email Valid", () -> {
            try {
                Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
                validateMethod.setAccessible(true);
                
                // Should not throw exception
                validateMethod.invoke(emailService, "valid@example.com");
                validateMethod.invoke(emailService, "mario.rossi+test@example-domain.com");
            } catch (Exception e) {
                if (e.getCause() != null) {
                    throw new RuntimeException(e.getCause());
                }
                throw new RuntimeException(e);
            }
        });
    }
    
    private void testValidateRecipientEmailInvalid() {
        runTest("Validate Recipient Email Invalid", () -> {
            try {
                Method validateMethod = EmailService.class.getDeclaredMethod("validateRecipientEmail", String.class);
                validateMethod.setAccessible(true);
                
                // Test invalid email format
                try {
                    validateMethod.invoke(emailService, "invalid-email");
                    fail("Should have thrown exception for invalid email format");
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    assertNotNull(cause, "Should have thrown an exception");
                    assertEquals("RECIPIENT_EMAIL_INVALID", getErrorCode(cause), "Should have correct error code");
                }
                
                // Test empty email
                try {
                    validateMethod.invoke(emailService, "");
                    fail("Should have thrown exception for empty email");
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    assertNotNull(cause, "Should have thrown an exception");
                    assertEquals("RECIPIENT_EMAIL_EMPTY", getErrorCode(cause), "Should have correct error code");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private void testExceptionHierarchy() {
        runTest("Exception Hierarchy", () -> {
            // Verify that custom exceptions extend the base EmailDeliveryException
            Class<?> baseClass = null;
            try {
                baseClass = Class.forName("it.odvsicilia.backend.exception.EmailDeliveryException");
            } catch (ClassNotFoundException e) {
                fail("EmailDeliveryException class not found");
            }
            
            assertExtendsBase(EmailSmtpException.class, baseClass);
            assertExtendsBase(EmailAuthenticationException.class, baseClass);
            assertExtendsBase(EmailApiTimeoutException.class, baseClass);
            assertExtendsBase(EmailInvalidRecipientException.class, baseClass);
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
    
    private void setPrivateField(String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = EmailService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(emailService, value);
        } catch (Exception e) {
            // Ignore - field may not exist in standalone test
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
            Method method = obj.getClass().getMethod(methodName, parameterTypes);
            assertNotNull(method, "Method " + methodName + " should exist");
        } catch (NoSuchMethodException e) {
            fail("Method " + methodName + " not found");
        }
    }
    
    private void assertExtendsBase(Class<?> derivedClass, Class<?> baseClass) {
        assertTrue(baseClass.isAssignableFrom(derivedClass), 
                  derivedClass.getSimpleName() + " should extend " + baseClass.getSimpleName());
    }
    
    private String getErrorCode(Throwable exception) {
        try {
            Method getErrorCodeMethod = exception.getClass().getMethod("getErrorCode");
            return (String) getErrorCodeMethod.invoke(exception);
        } catch (Exception e) {
            return null;
        }
    }
    
    @FunctionalInterface
    private interface TestRunnable {
        void run() throws Exception;
    }
}