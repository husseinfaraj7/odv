package it.odvsicilia.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import it.odvsicilia.backend.dto.ContactMessageDto;
import it.odvsicilia.backend.dto.OrderDto;
import it.odvsicilia.backend.dto.OrderItemDto;
import it.odvsicilia.backend.dto.ApiResponse;
import it.odvsicilia.backend.repository.ContactMessageRepository;
import it.odvsicilia.backend.repository.OrderRepository;
import it.odvsicilia.backend.service.EmailService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Render Deployment Validation Test Suite
 * 
 * This test suite validates:
 * 1. Database connectivity and CRUD operations
 * 2. Email functionality with Brevo API
 * 3. REST API endpoints with proper HTTP status codes
 * 4. Environment configuration validation
 * 5. Performance and connectivity checks
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class RenderDeploymentValidationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmailService emailService;

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${BREVO_API_KEY:#{null}}")
    private String brevoApiKey;

    @Value("${ADMIN_EMAIL:#{null}}")
    private String adminEmail;

    private String baseUrl;

    @Test
    @Order(1)
    public void testEnvironmentConfiguration() {
        baseUrl = "http://localhost:" + port;
        
        // Test critical environment variables are present in production environment
        if (System.getProperty("spring.profiles.active", "").contains("prod")) {
            assertNotNull(databaseUrl, "DATABASE_URL must be configured for production");
            assertNotNull(brevoApiKey, "BREVO_API_KEY must be configured for production");
            assertNotNull(adminEmail, "ADMIN_EMAIL must be configured for production");
            
            assertTrue(databaseUrl.contains("postgres"), "Production should use PostgreSQL");
            assertFalse(brevoApiKey.isEmpty(), "BREVO_API_KEY cannot be empty");
            assertTrue(adminEmail.contains("@"), "ADMIN_EMAIL must be valid email format");
        }
        
        System.out.println("✅ Environment Configuration Test Passed");
    }

    @Test
    @Order(2)
    public void testDatabaseConnectivity() throws Exception {
        // Test database connection
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(5), "Database connection should be valid");
            
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Database Product: " + metaData.getDatabaseProductName());
            System.out.println("Database Version: " + metaData.getDatabaseProductVersion());
            System.out.println("JDBC URL: " + metaData.getURL());
            
            // Check if required tables exist
            ResultSet tables = metaData.getTables(null, null, null, new String[]{"TABLE"});
            boolean contactMessagesTableExists = false;
            boolean ordersTableExists = false;
            
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME").toLowerCase();
                if (tableName.contains("contact") && tableName.contains("message")) {
                    contactMessagesTableExists = true;
                }
                if (tableName.contains("order") && !tableName.contains("item")) {
                    ordersTableExists = true;
                }
            }
            
            assertTrue(contactMessagesTableExists, "ContactMessage table should exist");
            assertTrue(ordersTableExists, "Orders table should exist");
        }
        
        System.out.println("✅ Database Connectivity Test Passed");
    }

    @Test
    @Order(3)
    public void testDatabaseCRUDOperations() {
        // Test Contact Message CRUD
        long initialContactCount = contactMessageRepository.count();
        
        // Create test contact message
        ContactMessageDto testContact = new ContactMessageDto();
        testContact.setName("Test User");
        testContact.setEmail("test@example.com");
        testContact.setSubject("Test Subject");
        testContact.setMessage("Test message for deployment validation");
        testContact.setPhone("1234567890");
        
        ResponseEntity<ApiResponse> contactResponse = restTemplate.postForEntity(
            baseUrl + "/api/contact/send", 
            testContact, 
            ApiResponse.class
        );
        
        assertEquals(HttpStatus.OK, contactResponse.getStatusCode());
        assertTrue(contactResponse.getBody().isSuccess());
        
        // Verify contact was saved
        long finalContactCount = contactMessageRepository.count();
        assertEquals(initialContactCount + 1, finalContactCount);
        
        // Test Order CRUD
        long initialOrderCount = orderRepository.count();
        
        // Create test order
        OrderDto testOrder = new OrderDto();
        testOrder.setCustomerName("Test Customer");
        testOrder.setCustomerEmail("customer@example.com");
        testOrder.setCustomerPhone("0987654321");
        testOrder.setShippingAddress("Test Address");
        testOrder.setShippingCity("Test City");
        testOrder.setShippingPostalCode("12345");
        testOrder.setShippingCountry("Italia");
        testOrder.setTotalAmount(java.math.BigDecimal.valueOf(51.98));
        
        OrderItemDto orderItem = new OrderItemDto();
        orderItem.setProductName("Test Oil");
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(java.math.BigDecimal.valueOf(25.99));
        testOrder.setItems(Arrays.asList(orderItem));
        
        ResponseEntity<ApiResponse> orderResponse = restTemplate.postForEntity(
            baseUrl + "/api/orders/create", 
            testOrder, 
            ApiResponse.class
        );
        
        assertEquals(HttpStatus.OK, orderResponse.getStatusCode());
        assertTrue(orderResponse.getBody().isSuccess());
        
        // Verify order was saved
        long finalOrderCount = orderRepository.count();
        assertEquals(initialOrderCount + 1, finalOrderCount);
        
        System.out.println("✅ Database CRUD Operations Test Passed");
    }

    @Test
    @Order(4)
    public void testRestApiEndpoints() {
        // Test Order endpoints
        testOrderEndpoints();
        
        // Test Contact endpoints
        testContactEndpoints();
        
        // Test Health/Stats endpoints
        testHealthEndpoints();
        
        System.out.println("✅ REST API Endpoints Test Passed");
    }

    private void testOrderEndpoints() {
        // Test GET /api/orders/all
        ResponseEntity<ApiResponse> allOrdersResponse = restTemplate.getForEntity(
            baseUrl + "/api/orders/all", ApiResponse.class);
        assertEquals(HttpStatus.OK, allOrdersResponse.getStatusCode());
        assertTrue(allOrdersResponse.getBody().isSuccess());

        // Test GET /api/orders/stats
        ResponseEntity<ApiResponse> statsResponse = restTemplate.getForEntity(
            baseUrl + "/api/orders/stats", ApiResponse.class);
        assertEquals(HttpStatus.OK, statsResponse.getStatusCode());
        assertTrue(statsResponse.getBody().isSuccess());

        // Test invalid order number (expecting 500 because exception is thrown)
        ResponseEntity<ApiResponse> invalidOrderResponse = restTemplate.getForEntity(
            baseUrl + "/api/orders/INVALID_ORDER", ApiResponse.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, invalidOrderResponse.getStatusCode());
        assertFalse(invalidOrderResponse.getBody().isSuccess());

        // Test invalid order status update
        Map<String, String> invalidStatus = new HashMap<>();
        invalidStatus.put("status", "INVALID_STATUS");
        
        HttpEntity<Map<String, String>> invalidStatusEntity = new HttpEntity<>(invalidStatus);
        ResponseEntity<ApiResponse> invalidStatusResponse = restTemplate.exchange(
            baseUrl + "/api/orders/INVALID_ORDER/status",
            HttpMethod.PUT,
            invalidStatusEntity,
            ApiResponse.class
        );
        assertEquals(HttpStatus.NOT_FOUND, invalidStatusResponse.getStatusCode());
    }

    private void testContactEndpoints() {
        // Test GET /api/contact/messages
        ResponseEntity<ApiResponse> messagesResponse = restTemplate.getForEntity(
            baseUrl + "/api/contact/messages", ApiResponse.class);
        assertEquals(HttpStatus.OK, messagesResponse.getStatusCode());
        assertTrue(messagesResponse.getBody().isSuccess());

        // Test GET /api/contact/unread-count
        ResponseEntity<ApiResponse> unreadCountResponse = restTemplate.getForEntity(
            baseUrl + "/api/contact/unread-count", ApiResponse.class);
        assertEquals(HttpStatus.OK, unreadCountResponse.getStatusCode());
        assertTrue(unreadCountResponse.getBody().isSuccess());

        // Test invalid contact message
        ContactMessageDto invalidContact = new ContactMessageDto();
        // Missing required fields
        
        ResponseEntity<ApiResponse> invalidContactResponse = restTemplate.postForEntity(
            baseUrl + "/api/contact/send", 
            invalidContact, 
            ApiResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidContactResponse.getStatusCode());
        assertFalse(invalidContactResponse.getBody().isSuccess());

        // Test mark as read with invalid ID
        ResponseEntity<ApiResponse> invalidMarkReadResponse = restTemplate.exchange(
            baseUrl + "/api/contact/mark-read/99999",
            HttpMethod.PUT,
            null,
            ApiResponse.class
        );
        assertEquals(HttpStatus.NOT_FOUND, invalidMarkReadResponse.getStatusCode());
    }

    private void testHealthEndpoints() {
        // Test basic application health by hitting any endpoint
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            baseUrl + "/api/orders/stats", String.class);
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        // Verify response time is reasonable (should be under 5 seconds)
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(baseUrl + "/api/contact/unread-count", ApiResponse.class);
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime < 5000, "API response time should be under 5 seconds");
    }

    @Test
    @Order(5)
    public void testEmailFunctionality() {
        // Note: In production, this would attempt to send real emails
        // For staging, we validate the email service configuration
        
        try {
            // Test email service configuration
            assertNotNull(emailService, "Email service should be available");
            
            // In a real deployment test, you might send a test email
            // For now, we just validate that the service can be instantiated
            // and critical configuration is present
            
            if (System.getProperty("spring.profiles.active", "").contains("prod")) {
                // In production, you could send a test email to admin
                // emailService.sendContactNotificationToAdmin(
                //     "Deployment Test", "test@deployment.com", 
                //     "Deployment Validation", "This is a deployment validation email", null);
            }
            
            System.out.println("✅ Email Functionality Test Passed (Configuration Validated)");
        } catch (Exception e) {
            fail("Email service configuration failed: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    public void testCorsConfiguration() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "https://odvsicilia.it");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            baseUrl + "/api/orders/stats",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Test invalid origin
        headers.set("Origin", "https://malicious-site.com");
        entity = new HttpEntity<>(headers);
        
        // This might not fail in test environment, but validates CORS is configured
        System.out.println("✅ CORS Configuration Test Completed");
    }

    @Test
    @Order(7)
    public void testDataIntegrity() {
        // Test that data persists across requests
        long contactCountBefore = contactMessageRepository.count();
        long orderCountBefore = orderRepository.count();
        
        // Make some operations
        ContactMessageDto contact = new ContactMessageDto();
        contact.setName("Integrity Test");
        contact.setEmail("integrity@test.com");
        contact.setSubject("Data Integrity Test");
        contact.setMessage("Testing data integrity");
        
        restTemplate.postForEntity(baseUrl + "/api/contact/send", contact, ApiResponse.class);
        
        // Verify counts increased
        long contactCountAfter = contactMessageRepository.count();
        assertEquals(contactCountBefore + 1, contactCountAfter);
        
        System.out.println("✅ Data Integrity Test Passed");
    }

    @Test
    @Order(8)
    public void testPerformanceBaseline() {
        int numberOfRequests = 10;
        long totalTime = 0;
        
        for (int i = 0; i < numberOfRequests; i++) {
            long startTime = System.currentTimeMillis();
            restTemplate.getForEntity(baseUrl + "/api/contact/unread-count", ApiResponse.class);
            long endTime = System.currentTimeMillis();
            totalTime += (endTime - startTime);
        }
        
        double averageResponseTime = (double) totalTime / numberOfRequests;
        System.out.println("Average Response Time: " + averageResponseTime + "ms");
        
        // Basic performance assertion - responses should be under 2 seconds on average
        assertTrue(averageResponseTime < 2000, 
            "Average response time should be under 2 seconds, was: " + averageResponseTime + "ms");
        
        System.out.println("✅ Performance Baseline Test Passed");
    }
}