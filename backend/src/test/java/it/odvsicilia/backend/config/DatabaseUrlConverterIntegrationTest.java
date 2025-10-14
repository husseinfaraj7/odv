package it.odvsicilia.backend.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseUrlConverter Integration Tests")
class DatabaseUrlConverterIntegrationTest {

    private DatabaseUrlConverter converter;
    private ConfigurableEnvironment environment;

    @BeforeEach
    void setUp() {
        converter = new DatabaseUrlConverter();
        environment = new StandardEnvironment();
        environment.getSystemProperties().remove("DATABASE_URL");
    }

    @Test
    @DisplayName("Should convert postgres:// scheme to jdbc:postgresql:// with Supabase direct connection")
    void testConvertPostgresSchemeSupabaseDirect() {
        String originalUrl = "postgres://myuser:mypass@db.abcdefghij1234567890.supabase.co:5432/postgres";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://myuser:mypass@db.abcdefghij1234567890.supabase.co:5432/postgres", jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("myuser:mypass@"));
        assertTrue(jdbcUrl.contains("db.abcdefghij1234567890.supabase.co"));
        assertTrue(jdbcUrl.contains(":5432/postgres"));
    }

    @Test
    @DisplayName("Should convert postgresql:// scheme to jdbc:postgresql:// with Supabase direct connection")
    void testConvertPostgresqlSchemeSupabaseDirect() {
        String originalUrl = "postgresql://myuser:mypass@db.xyz9876543210fedcba.supabase.co:5432/postgres";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://myuser:mypass@db.xyz9876543210fedcba.supabase.co:5432/postgres", jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("myuser:mypass@"));
        assertTrue(jdbcUrl.contains("db.xyz9876543210fedcba.supabase.co"));
        assertTrue(jdbcUrl.contains(":5432/postgres"));
    }

    @Test
    @DisplayName("Should convert postgres:// with Supabase pooler URL (aws-0)")
    void testConvertPostgresSchemeSupabasePoolerAws0() {
        String originalUrl = "postgres://pooluser:poolpass@aws-0-eu-north-1.pooler.supabase.com:6543/postgres";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://pooluser:poolpass@aws-0-eu-north-1.pooler.supabase.com:6543/postgres", jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("pooluser:poolpass@"));
        assertTrue(jdbcUrl.contains("aws-0-eu-north-1.pooler.supabase.com"));
        assertTrue(jdbcUrl.contains(":6543/postgres"));
    }

    @Test
    @DisplayName("Should convert postgresql:// with Supabase pooler URL (aws-1)")
    void testConvertPostgresqlSchemeSupabasePoolerAws1() {
        String originalUrl = "postgresql://admin:secret@aws-1-us-west-2.pooler.supabase.com:6543/postgres";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://admin:secret@aws-1-us-west-2.pooler.supabase.com:6543/postgres", jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("admin:secret@"));
        assertTrue(jdbcUrl.contains("aws-1-us-west-2.pooler.supabase.com"));
        assertTrue(jdbcUrl.contains(":6543/postgres"));
    }

    @Test
    @DisplayName("Should preserve query parameters like sslmode=require")
    void testPreserveQueryParametersSslMode() {
        String originalUrl = "postgres://user:pass@db.test12345678901234567.supabase.co:5432/postgres?sslmode=require";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://user:pass@db.test12345678901234567.supabase.co:5432/postgres?sslmode=require", jdbcUrl);
        assertTrue(jdbcUrl.contains("?sslmode=require"));
    }

    @Test
    @DisplayName("Should preserve multiple query parameters")
    void testPreserveMultipleQueryParameters() {
        String originalUrl = "postgres://user:pass@db.example.supabase.co:5432/postgres?sslmode=require&connect_timeout=10";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://user:pass@db.example.supabase.co:5432/postgres?sslmode=require&connect_timeout=10", jdbcUrl);
        assertTrue(jdbcUrl.contains("?sslmode=require&connect_timeout=10"));
    }

    @Test
    @DisplayName("Should preserve complex passwords with special characters")
    void testPreserveComplexPasswordsWithSpecialCharacters() {
        String originalUrl = "postgres://user:P@ssw0rd!#$%@db.test12345678901234567.supabase.co:5432/postgres";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://user:P@ssw0rd!#$%@db.test12345678901234567.supabase.co:5432/postgres", jdbcUrl);
        assertTrue(jdbcUrl.contains("user:P@ssw0rd!#$%@"));
    }

    @Test
    @DisplayName("Should handle direct connection URLs with different project references")
    void testDirectConnectionUrlsVariousProjectRefs() {
        String[] urls = {
            "postgres://user:pass@db.proj1234567890123456.supabase.co:5432/postgres",
            "postgres://user:pass@db.abcabcabcabcabcabcab.supabase.co:5432/postgres",
            "postgres://user:pass@db.xyzxyzxyzxyzxyzxyzxy.supabase.co:5432/postgres"
        };
        
        for (String originalUrl : urls) {
            environment = new StandardEnvironment();
            environment.getSystemProperties().put("DATABASE_URL", originalUrl);
            
            converter.postProcessEnvironment(environment, new SpringApplication());
            
            String jdbcUrl = environment.getProperty("spring.datasource.url");
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
            assertTrue(jdbcUrl.contains("user:pass@"));
            assertTrue(jdbcUrl.contains(".supabase.co:5432/postgres"));
        }
    }

    @Test
    @DisplayName("Should handle pooler URLs with different regions")
    void testPoolerUrlsVariousRegions() {
        String[] urls = {
            "postgres://user:pass@aws-0-us-east-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-0-eu-west-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-1-ca-central-1.pooler.supabase.com:6543/postgres"
        };
        
        for (String originalUrl : urls) {
            environment = new StandardEnvironment();
            environment.getSystemProperties().put("DATABASE_URL", originalUrl);
            
            converter.postProcessEnvironment(environment, new SpringApplication());
            
            String jdbcUrl = environment.getProperty("spring.datasource.url");
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
            assertTrue(jdbcUrl.contains("user:pass@"));
            assertTrue(jdbcUrl.contains(".pooler.supabase.com:6543/postgres"));
        }
    }

    @Test
    @DisplayName("Should preserve different port numbers")
    void testPreserveDifferentPorts() {
        String directUrl = "postgres://user:pass@db.test.supabase.co:5432/postgres";
        String poolerUrl = "postgres://user:pass@aws-0-region.pooler.supabase.com:6543/postgres";
        
        environment.getSystemProperties().put("DATABASE_URL", directUrl);
        converter.postProcessEnvironment(environment, new SpringApplication());
        String jdbcUrlDirect = environment.getProperty("spring.datasource.url");
        assertTrue(jdbcUrlDirect.contains(":5432/"));
        
        environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", poolerUrl);
        converter.postProcessEnvironment(environment, new SpringApplication());
        String jdbcUrlPooler = environment.getProperty("spring.datasource.url");
        assertTrue(jdbcUrlPooler.contains(":6543/"));
    }

    @Test
    @DisplayName("Should preserve different database names")
    void testPreserveDifferentDatabaseNames() {
        String url1 = "postgres://user:pass@db.test.supabase.co:5432/postgres";
        String url2 = "postgres://user:pass@db.test.supabase.co:5432/mydb";
        
        environment.getSystemProperties().put("DATABASE_URL", url1);
        converter.postProcessEnvironment(environment, new SpringApplication());
        String jdbcUrl1 = environment.getProperty("spring.datasource.url");
        assertTrue(jdbcUrl1.endsWith("/postgres"));
        
        environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", url2);
        converter.postProcessEnvironment(environment, new SpringApplication());
        String jdbcUrl2 = environment.getProperty("spring.datasource.url");
        assertTrue(jdbcUrl2.endsWith("/mydb"));
    }

    @Test
    @DisplayName("Should preserve different usernames")
    void testPreserveDifferentUsernames() {
        String[] urls = {
            "postgres://admin:pass@db.test.supabase.co:5432/postgres",
            "postgres://developer:pass@db.test.supabase.co:5432/postgres",
            "postgres://readonly:pass@db.test.supabase.co:5432/postgres"
        };
        
        for (String originalUrl : urls) {
            environment = new StandardEnvironment();
            environment.getSystemProperties().put("DATABASE_URL", originalUrl);
            
            converter.postProcessEnvironment(environment, new SpringApplication());
            
            String jdbcUrl = environment.getProperty("spring.datasource.url");
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.contains(originalUrl.substring("postgres://".length())));
        }
    }

    @Test
    @DisplayName("Should set spring.datasource.url for already JDBC formatted URLs")
    void testSetsJdbcUrlsDirectly() {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/testdb";
        environment.getSystemProperties().put("DATABASE_URL", jdbcUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resultUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(resultUrl);
        assertEquals(jdbcUrl, resultUrl);
    }

    @Test
    @DisplayName("Should not modify URLs with other schemes")
    void testDoesNotModifyOtherSchemes() {
        String mysqlUrl = "mysql://user:pass@localhost:3306/testdb";
        environment.getSystemProperties().put("DATABASE_URL", mysqlUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resultUrl = environment.getProperty("spring.datasource.url");
        assertNull(resultUrl);
    }

    @Test
    @DisplayName("Should handle empty DATABASE_URL")
    void testHandleEmptyDatabaseUrl() {
        environment.getSystemProperties().put("DATABASE_URL", "");
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNull(jdbcUrl);
    }

    @Test
    @DisplayName("Should handle null DATABASE_URL")
    void testHandleNullDatabaseUrl() {
        ConfigurableEnvironment cleanEnv = new StandardEnvironment();
        converter.postProcessEnvironment(cleanEnv, new SpringApplication());
        
        String jdbcUrl = cleanEnv.getProperty("spring.datasource.url");
        assertNull(jdbcUrl);
    }

    @Test
    @DisplayName("Should pass through jdbc:postgresql:// URLs unchanged and log appropriate message")
    void testPassThroughJdbcPostgresqlUrl() {
        Logger logger = (Logger) LoggerFactory.getLogger(DatabaseUrlConverter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        String jdbcUrl = "jdbc:postgresql://localhost:5432/testdb?user=testuser&password=testpass";
        environment.getSystemProperties().put("DATABASE_URL", jdbcUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resultUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(resultUrl);
        assertEquals(jdbcUrl, resultUrl);
        
        boolean foundLogMessage = listAppender.list.stream()
            .anyMatch(event -> 
                event.getMessage().contains("Database URL is already in JDBC format"));
        
        assertTrue(foundLogMessage, "Expected log message about JDBC format detection was not found");
        
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Converted spring.datasource.url should override application.properties fallback to DATABASE_URL")
    void testConvertedPropertyOverridesFallback() {
        String rawDatabaseUrl = "postgres://testuser:testpass@db.example.supabase.co:5432/postgres";
        environment.getSystemProperties().put("DATABASE_URL", rawDatabaseUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(resolvedUrl, "Resolved spring.datasource.url should not be null");
        assertTrue(resolvedUrl.startsWith("jdbc:postgresql://"), 
            "Resolved URL should be in JDBC format, not raw DATABASE_URL");
        assertEquals("jdbc:postgresql://testuser:testpass@db.example.supabase.co:5432/postgres", resolvedUrl,
            "Converted URL should take precedence over fallback to raw DATABASE_URL");
        assertFalse(resolvedUrl.startsWith("postgres://"), 
            "Should not resolve to unconverted postgres:// format");
    }

    @Test
    @DisplayName("Should convert postgres://user:pass@host:port/db to jdbc:postgresql:// with credentials extracted")
    void testConversionWithEmbeddedCredentials() {
        String originalUrl = "postgres://dbuser:secret123@aws-0-eu-central-1.pooler.supabase.com:6543/maindb";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "Converted URL should not be null");
        assertEquals("jdbc:postgresql://dbuser:secret123@aws-0-eu-central-1.pooler.supabase.com:6543/maindb", 
            convertedUrl, "URL should be converted to JDBC format with credentials preserved");
        assertTrue(convertedUrl.startsWith("jdbc:postgresql://"), "URL should have jdbc:postgresql:// prefix");
        assertTrue(convertedUrl.contains("dbuser:secret123@"), "Credentials should be embedded in URL");
        assertTrue(convertedUrl.contains("aws-0-eu-central-1.pooler.supabase.com:6543/maindb"), 
            "Host, port, and database should be preserved after credentials");
    }

    @Test
    @DisplayName("Should convert postgresql://user:pass@host:port/db format with credential extraction")
    void testConversionPostgresqlSchemeWithCredentials() {
        String originalUrl = "postgresql://appuser:p@ssw0rd!@db.xyz123456789012345678.supabase.co:5432/production";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "Converted URL should not be null");
        assertEquals("jdbc:postgresql://appuser:p@ssw0rd!@db.xyz123456789012345678.supabase.co:5432/production",
            convertedUrl, "postgresql:// URL should be converted with credentials preserved");
        assertTrue(convertedUrl.startsWith("jdbc:postgresql://"), "URL should have jdbc:postgresql:// prefix");
        assertTrue(convertedUrl.contains("appuser:p@ssw0rd!@"), "Complex credentials should be preserved");
    }

    @Test
    @DisplayName("Spring property resolution should retrieve JDBC-formatted URL not unconverted DATABASE_URL")
    void testSpringPropertyResolutionReturnsJdbcFormat() {
        String unconvertedUrl = "postgres://admin:adminpass@localhost:5432/testdb";
        String expectedJdbcUrl = "jdbc:postgresql://admin:adminpass@localhost:5432/testdb";
        
        environment.getSystemProperties().put("DATABASE_URL", unconvertedUrl);
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        
        assertNotNull(resolvedUrl, "Spring property resolution should return a value");
        assertEquals(expectedJdbcUrl, resolvedUrl, 
            "Property resolution should return converted JDBC URL");
        assertNotEquals(unconvertedUrl, resolvedUrl, 
            "Property resolution should NOT return unconverted DATABASE_URL");
        assertTrue(resolvedUrl.startsWith("jdbc:"), 
            "Resolved URL must be in JDBC format for Spring datasource");
    }

    @Test
    @DisplayName("EnvironmentPostProcessor-added properties should override application property defaults")
    void testPropertySourceOrderingOverridesDefaults() {
        String postgresUrl = "postgres://override:pass@override.example.com:5432/overridedb";
        String expectedJdbcUrl = "jdbc:postgresql://override:pass@override.example.com:5432/overridedb";
        
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("spring.datasource.url", "jdbc:h2:mem:defaultdb");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("defaultProperties", defaultProps)
        );
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(resolvedUrl, "URL should be resolved from property sources");
        assertEquals(expectedJdbcUrl, resolvedUrl,
            "EnvironmentPostProcessor-added property should override default property");
        assertNotEquals("jdbc:h2:mem:defaultdb", resolvedUrl,
            "Default property should be overridden, not used");
    }

    @Test
    @DisplayName("Converter should handle credential extraction before Spring datasource parses hostname")
    void testCredentialExtractionBeforeDatasourceProcessing() {
        String urlWithCredentials = "postgres://user123:p@ss:with:colons@db.project.supabase.co:5432/postgres";
        environment.getSystemProperties().put("DATABASE_URL", urlWithCredentials);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "Converted URL should be present");
        assertTrue(convertedUrl.startsWith("jdbc:postgresql://"), 
            "URL should be in JDBC format");
        assertTrue(convertedUrl.contains("user123:p@ss:with:colons@"), 
            "Complex credentials with colons should be preserved in the authority section");
        assertTrue(convertedUrl.contains("db.project.supabase.co:5432/postgres"),
            "Hostname, port, and database should follow credentials correctly");
    }

    @Test
    @DisplayName("Property source ordering: EnvironmentPostProcessor properties should be first")
    void testPropertySourceOrderingIsFirst() {
        String postgresUrl = "postgres://firstuser:firstpass@first.example.com:5432/firstdb";
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        Map<String, Object> laterProps = new HashMap<>();
        laterProps.put("spring.datasource.url", "jdbc:postgresql://lateruser:laterpass@later.example.com:5432/laterdb");
        environment.getPropertySources().addFirst(
            new org.springframework.core.env.MapPropertySource("laterProperties", laterProps)
        );
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertEquals("jdbc:postgresql://firstuser:firstpass@first.example.com:5432/firstdb", resolvedUrl,
            "EnvironmentPostProcessor should add properties at the very first position");
    }

    @Test
    @DisplayName("Should preserve URL-encoded special characters in credentials")
    void testUrlEncodedCredentialsPreservation() {
        String urlWithEncodedChars = "postgres://user%40domain:p%40ss%21word%23@db.test.supabase.co:5432/testdb";
        environment.getSystemProperties().put("DATABASE_URL", urlWithEncodedChars);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl);
        assertEquals("jdbc:postgresql://user%40domain:p%40ss%21word%23@db.test.supabase.co:5432/testdb", convertedUrl,
            "URL-encoded characters in credentials should be preserved during conversion");
        assertTrue(convertedUrl.contains("user%40domain:p%40ss%21word%23@"),
            "Encoded credentials should remain encoded for proper JDBC parsing");
    }

    @Test
    @DisplayName("Converted JDBC URL should work with Spring datasource without manual credential extraction")
    void testJdbcUrlContainsCredentialsForSpringDatasource() {
        String originalUrl = "postgres://springuser:springpass@db.prod.supabase.co:5432/proddb?sslmode=require";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl, "JDBC URL should be present for Spring datasource");
        assertTrue(jdbcUrl.contains("springuser:springpass@"),
            "Credentials must be embedded in JDBC URL for Spring datasource to extract");
        assertTrue(jdbcUrl.contains("db.prod.supabase.co:5432/proddb"),
            "Hostname, port, and database must be correctly positioned after credentials");
        assertTrue(jdbcUrl.contains("?sslmode=require"),
            "Query parameters should be preserved for datasource configuration");
    }

    @Test
    @DisplayName("Multiple conversions should maintain property source precedence")
    void testMultipleConversionsPropertyPrecedence() {
        String firstUrl = "postgres://first:pass1@host1.supabase.co:5432/db1";
        environment.getSystemProperties().put("DATABASE_URL", firstUrl);
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String firstResolved = environment.getProperty("spring.datasource.url");
        assertEquals("jdbc:postgresql://first:pass1@host1.supabase.co:5432/db1", firstResolved);
        
        environment = new StandardEnvironment();
        String secondUrl = "postgres://second:pass2@host2.supabase.co:5432/db2";
        environment.getSystemProperties().put("DATABASE_URL", secondUrl);
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String secondResolved = environment.getProperty("spring.datasource.url");
        assertEquals("jdbc:postgresql://second:pass2@host2.supabase.co:5432/db2", secondResolved,
            "Each environment should have its own converted property with correct precedence");
    }

    @Test
    @DisplayName("Should handle credentials with @ symbol in password")
    void testCredentialsWithAtSymbolInPassword() {
        String urlWithAtInPassword = "postgres://myuser:p@ssw0rd@@db.test.supabase.co:5432/postgres";
        environment.getSystemProperties().put("DATABASE_URL", urlWithAtInPassword);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl);
        assertEquals("jdbc:postgresql://myuser:p@ssw0rd@@db.test.supabase.co:5432/postgres", convertedUrl,
            "Password with @ symbol should be preserved correctly");
        assertTrue(convertedUrl.contains("myuser:p@ssw0rd@@"),
            "Complex password with @ should be maintained in credential section");
    }

    @Test
    @DisplayName("Comprehensive: Converted property takes absolute precedence over application.properties DATABASE_URL fallback")
    void testConvertedPropertyTakesPrecedenceOverApplicationPropertiesFallback() {
        String rawDatabaseUrl = "postgres://envuser:envpass@env.host.supabase.co:5432/envdb";
        String expectedJdbcUrl = "jdbc:postgresql://envuser:envpass@env.host.supabase.co:5432/envdb";
        
        Map<String, Object> appPropertiesSimulated = new HashMap<>();
        appPropertiesSimulated.put("spring.datasource.url", "${DATABASE_URL:jdbc:h2:mem:testdb}");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("applicationProperties", appPropertiesSimulated)
        );
        
        environment.getSystemProperties().put("DATABASE_URL", rawDatabaseUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(resolvedUrl, "spring.datasource.url must be resolved");
        assertEquals(expectedJdbcUrl, resolvedUrl,
            "Converted JDBC URL from EnvironmentPostProcessor must override application.properties fallback");
        assertFalse(resolvedUrl.equals(rawDatabaseUrl),
            "Must not resolve to raw DATABASE_URL value from fallback");
        assertFalse(resolvedUrl.contains("${DATABASE_URL"),
            "Must not contain placeholder expression");
        assertTrue(resolvedUrl.startsWith("jdbc:postgresql://"),
            "Must be proper JDBC format, proving conversion took precedence");
    }

    @Test
    @DisplayName("Comprehensive: Credential extraction from postgres://user:pass@host format to jdbc:postgresql://")
    void testCredentialExtractionComprehensive() {
        String[] testCases = {
            "postgres://simple:password@host.example.com:5432/db",
            "postgres://user_name:pass_word@host-name.example.com:5432/database",
            "postgres://admin:p@$$w0rd!@pooler.supabase.com:6543/prod",
            "postgresql://test.user:test.pass@test.host.com:5432/testdb"
        };
        
        String[] expectedJdbcUrls = {
            "jdbc:postgresql://simple:password@host.example.com:5432/db",
            "jdbc:postgresql://user_name:pass_word@host-name.example.com:5432/database",
            "jdbc:postgresql://admin:p@$$w0rd!@pooler.supabase.com:6543/prod",
            "jdbc:postgresql://test.user:test.pass@test.host.com:5432/testdb"
        };
        
        for (int i = 0; i < testCases.length; i++) {
            environment = new StandardEnvironment();
            String originalUrl = testCases[i];
            String expectedUrl = expectedJdbcUrls[i];
            
            environment.getSystemProperties().put("DATABASE_URL", originalUrl);
            converter.postProcessEnvironment(environment, new SpringApplication());
            
            String convertedUrl = environment.getProperty("spring.datasource.url");
            assertNotNull(convertedUrl, "Converted URL should not be null for: " + originalUrl);
            assertEquals(expectedUrl, convertedUrl,
                "Credentials should be properly extracted and preserved in JDBC format");
            
            String[] parts = expectedUrl.split("@");
            assertTrue(parts.length >= 2, "URL should contain @ separator between credentials and host");
            assertTrue(parts[0].contains("://"), "First part should contain scheme and credentials");
            assertTrue(parts[0].contains(":"), "Credentials section should contain username:password separator");
        }
    }

    @Test
    @DisplayName("Comprehensive: Spring property resolution retrieves JDBC URL not raw DATABASE_URL")
    void testPropertyResolutionRetrievesConvertedNotRaw() {
        ConfigurableEnvironment testEnv = new StandardEnvironment();
        String rawUrl = "postgres://proptest:testpass@property.test.com:5432/propdb?sslmode=require";
        String expectedJdbc = "jdbc:postgresql://proptest:testpass@property.test.com:5432/propdb?sslmode=require";
        
        testEnv.getSystemProperties().put("DATABASE_URL", rawUrl);
        
        DatabaseUrlConverter localConverter = new DatabaseUrlConverter();
        localConverter.postProcessEnvironment(testEnv, new SpringApplication());
        
        String resolvedDatasourceUrl = testEnv.getProperty("spring.datasource.url");
        String resolvedDatabaseUrl = testEnv.getProperty("DATABASE_URL");
        
        assertNotNull(resolvedDatasourceUrl, "spring.datasource.url should be resolved");
        assertNotNull(resolvedDatabaseUrl, "DATABASE_URL should still be accessible");
        
        assertEquals(expectedJdbc, resolvedDatasourceUrl,
            "spring.datasource.url must resolve to converted JDBC format");
        assertEquals(rawUrl, resolvedDatabaseUrl,
            "DATABASE_URL should remain unchanged in its original format");
        assertNotEquals(resolvedDatasourceUrl, resolvedDatabaseUrl,
            "The two properties must have different values (converted vs raw)");
        
        assertTrue(resolvedDatasourceUrl.startsWith("jdbc:"),
            "Datasource URL must be JDBC format for Spring to parse");
        assertTrue(resolvedDatabaseUrl.startsWith("postgres://"),
            "DATABASE_URL should remain in original postgres:// format");
    }

    @Test
    @DisplayName("Comprehensive: Property source ordering ensures EnvironmentPostProcessor overrides defaults")
    void testPropertySourceOrderingEnsuresOverride() {
        String postgresUrl = "postgres://ordertest:orderpass@order.example.com:5432/orderdb";
        String expectedJdbc = "jdbc:postgresql://ordertest:orderpass@order.example.com:5432/orderdb";
        
        Map<String, Object> defaultApplicationProps = new HashMap<>();
        defaultApplicationProps.put("spring.datasource.url", "jdbc:h2:mem:defaultdb");
        defaultApplicationProps.put("spring.datasource.driver-class-name", "org.h2.Driver");
        
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("applicationConfig", defaultApplicationProps)
        );
        
        Map<String, Object> commandLineProps = new HashMap<>();
        commandLineProps.put("spring.profiles.active", "test");
        environment.getPropertySources().addFirst(
            new org.springframework.core.env.MapPropertySource("commandLineArgs", commandLineProps)
        );
        
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        int propertySourceCountBefore = environment.getPropertySources().size();
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        int propertySourceCountAfter = environment.getPropertySources().size();
        assertTrue(propertySourceCountAfter > propertySourceCountBefore,
            "EnvironmentPostProcessor should add a new property source");
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertEquals(expectedJdbc, resolvedUrl,
            "Converted URL should override default application properties");
        assertNotEquals("jdbc:h2:mem:defaultdb", resolvedUrl,
            "Default H2 URL should be overridden");
        
        org.springframework.core.env.PropertySource<?> firstPropertySource = 
            environment.getPropertySources().iterator().next();
        assertEquals("databaseUrlConversion", firstPropertySource.getName(),
            "DatabaseUrlConverter should add its property source at the first position");
    }

    @Test
    @DisplayName("Comprehensive: Credential extraction happens before datasource hostname parsing")
    void testCredentialExtractionBeforeHostnameParsing() {
        String complexUrl = "postgres://user.name:p@ss:w0rd:with:multiple:colons@db.complex-host.supabase.co:5432/complexdb?sslmode=require&connectTimeout=10";
        environment.getSystemProperties().put("DATABASE_URL", complexUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "Converted URL must be present");
        
        String expectedJdbc = "jdbc:postgresql://user.name:p@ss:w0rd:with:multiple:colons@db.complex-host.supabase.co:5432/complexdb?sslmode=require&connectTimeout=10";
        assertEquals(expectedJdbc, convertedUrl,
            "Complex credentials with colons should be preserved without interfering with hostname parsing");
        
        assertTrue(convertedUrl.contains("user.name:p@ss:w0rd:with:multiple:colons@"),
            "All credential components including multiple colons should be in the authority section");
        
        assertTrue(convertedUrl.contains("@db.complex-host.supabase.co:5432/complexdb"),
            "Hostname should immediately follow the last @ of credentials");
        
        int lastAtIndex = convertedUrl.lastIndexOf("@");
        String afterAt = convertedUrl.substring(lastAtIndex + 1);
        assertTrue(afterAt.startsWith("db.complex-host.supabase.co"),
            "After final @, should be hostname not credentials");
        
        String hostPortDb = afterAt.split("\\?")[0];
        assertEquals("db.complex-host.supabase.co:5432/complexdb", hostPortDb,
            "Host:port/database extraction should work correctly after credential section");
    }

    @Test
    @DisplayName("Comprehensive: Converted property precedence in multi-source environment")
    void testConvertedPropertyPrecedenceInMultiSourceEnvironment() {
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("spring.datasource.url", "jdbc:h2:mem:default");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("defaultProperties", defaultProps)
        );
        
        Map<String, Object> configFileProps = new HashMap<>();
        configFileProps.put("spring.datasource.url", "jdbc:postgresql://config:configpass@config.host:5432/configdb");
        environment.getPropertySources().addFirst(
            new org.springframework.core.env.MapPropertySource("applicationConfig", configFileProps)
        );
        
        String envDatabaseUrl = "postgres://env:envpass@env.host.supabase.co:5432/envdb";
        String expectedJdbc = "jdbc:postgresql://env:envpass@env.host.supabase.co:5432/envdb";
        environment.getSystemProperties().put("DATABASE_URL", envDatabaseUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertEquals(expectedJdbc, resolvedUrl,
            "EnvironmentPostProcessor-converted URL should take precedence over all other sources");
        
        org.springframework.core.env.PropertySource<?> firstSource = 
            environment.getPropertySources().iterator().next();
        Object valueFromFirstSource = firstSource.getProperty("spring.datasource.url");
        assertEquals(expectedJdbc, valueFromFirstSource,
            "First property source should contain the converted JDBC URL");
    }

    @Test
    @DisplayName("Comprehensive: Verify JDBC format is valid for Spring DataSource connection")
    void testJdbcFormatValidForSpringDataSource() {
        String[] postgresUrls = {
            "postgres://validuser:validpass@valid.host.com:5432/validdb",
            "postgresql://testuser:testpass@test.supabase.co:6543/testdb?sslmode=require",
            "postgres://prod.user:pr0d!p@ss@prod.pooler.supabase.com:6543/production?sslmode=require&connectTimeout=30"
        };
        
        String[] expectedJdbcPatterns = {
            "^jdbc:postgresql://validuser:validpass@valid\\.host\\.com:5432/validdb$",
            "^jdbc:postgresql://testuser:testpass@test\\.supabase\\.co:6543/testdb\\?sslmode=require$",
            "^jdbc:postgresql://prod\\.user:pr0d!p@ss@prod\\.pooler\\.supabase\\.com:6543/production\\?sslmode=require&connectTimeout=30$"
        };
        
        for (int i = 0; i < postgresUrls.length; i++) {
            environment = new StandardEnvironment();
            environment.getSystemProperties().put("DATABASE_URL", postgresUrls[i]);
            
            converter.postProcessEnvironment(environment, new SpringApplication());
            
            String jdbcUrl = environment.getProperty("spring.datasource.url");
            assertNotNull(jdbcUrl, "JDBC URL should be generated");
            
            assertTrue(jdbcUrl.matches(expectedJdbcPatterns[i]),
                "JDBC URL should match expected pattern for Spring DataSource: " + jdbcUrl);
            
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"),
                "Must have correct JDBC PostgreSQL prefix");
            
            assertTrue(jdbcUrl.contains("@"),
                "Must contain @ separator for embedded credentials");
            
            String afterScheme = jdbcUrl.substring("jdbc:postgresql://".length());
            int lastAtIndex = afterScheme.lastIndexOf("@");
            assertTrue(lastAtIndex > 0, "URL must contain @ separator");
            
            String credentialsPart = afterScheme.substring(0, lastAtIndex);
            String hostSection = afterScheme.substring(lastAtIndex + 1);
            
            assertTrue(credentialsPart.contains(":"),
                "Credentials section must contain username:password separator");
            
            assertTrue(hostSection.matches("^[a-zA-Z0-9.\\-]+:[0-9]+/[a-zA-Z0-9_\\-]+.*$"),
                "Host section must follow format: host:port/database[?params]");
        }
    }

    @Test
    @DisplayName("Comprehensive: Multiple credentials formats with proper URL encoding")
    void testMultipleCredentialsFormatsWithUrlEncoding() {
        Map<String, String> urlPairs = new HashMap<>();
        urlPairs.put(
            "postgres://user123:pass123@host1.com:5432/db1",
            "jdbc:postgresql://user123:pass123@host1.com:5432/db1"
        );
        urlPairs.put(
            "postgres://user%40email:pass%23word@host2.com:5432/db2",
            "jdbc:postgresql://user%40email:pass%23word@host2.com:5432/db2"
        );
        urlPairs.put(
            "postgresql://admin:p@ss%2Fword@host3.com:5432/db3",
            "jdbc:postgresql://admin:p@ss%2Fword@host3.com:5432/db3"
        );
        
        urlPairs.forEach((postgresUrl, expectedJdbc) -> {
            ConfigurableEnvironment testEnv = new StandardEnvironment();
            testEnv.getSystemProperties().put("DATABASE_URL", postgresUrl);
            
            converter.postProcessEnvironment(testEnv, new SpringApplication());
            
            String convertedUrl = testEnv.getProperty("spring.datasource.url");
            assertEquals(expectedJdbc, convertedUrl,
                "URL-encoded credentials should be preserved exactly during conversion");
            
            if (postgresUrl.contains("%")) {
                int lastAt = convertedUrl.lastIndexOf("@");
                String credentialsPart = convertedUrl.substring("jdbc:postgresql://".length(), lastAt);
                assertTrue(credentialsPart.contains("%"),
                    "URL encoding should be preserved in credentials");
            }
        });
    }

    @Test
    @DisplayName("Comprehensive: Property resolution with DATABASE_URL fallback simulation")
    void testPropertyResolutionWithDatabaseUrlFallbackSimulation() {
        String rawDatabaseUrl = "postgres://fallback:fallbackpass@fallback.host.com:5432/fallbackdb";
        String expectedConvertedUrl = "jdbc:postgresql://fallback:fallbackpass@fallback.host.com:5432/fallbackdb";
        
        environment.getSystemProperties().put("DATABASE_URL", rawDatabaseUrl);
        
        Map<String, Object> applicationProps = new HashMap<>();
        applicationProps.put("spring.datasource.url", "${DATABASE_URL}");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("applicationConfig", applicationProps)
        );
        
        String beforeConversion = environment.getProperty("spring.datasource.url");
        assertTrue(beforeConversion == null || beforeConversion.equals(rawDatabaseUrl) || beforeConversion.equals("${DATABASE_URL}"),
            "Before conversion, spring.datasource.url resolves to raw DATABASE_URL or placeholder");
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String afterConversion = environment.getProperty("spring.datasource.url");
        assertNotNull(afterConversion, "After conversion, URL should be resolved");
        assertEquals(expectedConvertedUrl, afterConversion,
            "Converted JDBC URL should override the DATABASE_URL fallback");
        assertNotEquals(rawDatabaseUrl, afterConversion,
            "Should NOT return raw DATABASE_URL value");
        assertTrue(afterConversion.startsWith("jdbc:"),
            "Must be in JDBC format, proving converter added proper override");
    }

    @Test
    @DisplayName("Comprehensive: Credential extraction with edge cases")
    void testCredentialExtractionEdgeCases() {
        Map<String, String> edgeCases = new HashMap<>();
        edgeCases.put(
            "postgres://u:p@host:5432/db",
            "jdbc:postgresql://u:p@host:5432/db"
        );
        edgeCases.put(
            "postgres://user:@host:5432/db",
            "jdbc:postgresql://user:@host:5432/db"
        );
        edgeCases.put(
            "postgres://:password@host:5432/db",
            "jdbc:postgresql://:password@host:5432/db"
        );
        edgeCases.put(
            "postgres://user.with.dots:pass.with.dots@host.with.dots:5432/db.with.dots",
            "jdbc:postgresql://user.with.dots:pass.with.dots@host.with.dots:5432/db.with.dots"
        );
        edgeCases.put(
            "postgresql://user-with-dash:pass-with-dash@host-with-dash:5432/db-with-dash",
            "jdbc:postgresql://user-with-dash:pass-with-dash@host-with-dash:5432/db-with-dash"
        );
        
        edgeCases.forEach((input, expected) -> {
            environment = new StandardEnvironment();
            environment.getSystemProperties().put("DATABASE_URL", input);
            
            converter.postProcessEnvironment(environment, new SpringApplication());
            
            String converted = environment.getProperty("spring.datasource.url");
            assertEquals(expected, converted,
                "Edge case credential format should be handled correctly: " + input);
        });
    }

    @Test
    @DisplayName("Enhanced: Converted property strictly overrides application.properties DATABASE_URL fallback")
    void testConvertedPropertyStrictlyOverridesApplicationPropertiesFallback() {
        String rawDatabaseUrl = "postgres://priority:prioritypass@priority.host.supabase.co:5432/prioritydb?sslmode=require";
        String expectedJdbcUrl = "jdbc:postgresql://priority:prioritypass@priority.host.supabase.co:5432/prioritydb?sslmode=require";
        
        Map<String, Object> simulatedApplicationProperties = new HashMap<>();
        simulatedApplicationProperties.put("spring.datasource.url", "${spring.datasource.url:${DATABASE_URL}}");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("applicationConfig: [classpath:/application.properties]", simulatedApplicationProperties)
        );
        
        environment.getSystemProperties().put("DATABASE_URL", rawDatabaseUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(resolvedUrl, "spring.datasource.url must resolve to a value");
        assertEquals(expectedJdbcUrl, resolvedUrl,
            "EnvironmentPostProcessor-converted JDBC URL must take absolute precedence");
        assertNotEquals(rawDatabaseUrl, resolvedUrl,
            "Must NOT fall back to raw postgres:// DATABASE_URL format");
        assertFalse(resolvedUrl.contains("${"), "Must not contain unresolved placeholders");
        assertTrue(resolvedUrl.startsWith("jdbc:postgresql://"),
            "Must be fully converted JDBC format proving precedence over fallback");
        
        String databaseUrlValue = environment.getProperty("DATABASE_URL");
        assertEquals(rawDatabaseUrl, databaseUrlValue,
            "DATABASE_URL itself should remain unchanged in environment");
        assertNotEquals(resolvedUrl, databaseUrlValue,
            "spring.datasource.url and DATABASE_URL must have different values");
    }

    @Test
    @DisplayName("Enhanced: Postgres URL with credentials converted to JDBC with extracted username and password")
    void testPostgresUrlWithCredentialsConvertedToJdbcWithExtraction() {
        String originalUrl = "postgres://dbadmin:s3cr3tPass@aws-0-eu-west-1.pooler.supabase.com:6543/production";
        environment.getSystemProperties().put("DATABASE_URL", originalUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "Converted URL must be present");
        assertEquals("jdbc:postgresql://dbadmin:s3cr3tPass@aws-0-eu-west-1.pooler.supabase.com:6543/production",
            convertedUrl, "Credentials must be properly extracted and embedded in JDBC URL");
        
        assertTrue(convertedUrl.startsWith("jdbc:postgresql://"),
            "URL must have proper JDBC PostgreSQL scheme");
        
        String authority = convertedUrl.substring("jdbc:postgresql://".length()).split("/")[0].split("\\?")[0];
        assertTrue(authority.contains("@"), "Authority section must contain @ separator");
        
        String[] authorityParts = authority.split("@");
        assertEquals(2, authorityParts.length, "Authority should have credentials@host format");
        
        String credentials = authorityParts[0];
        String hostPort = authorityParts[1];
        
        assertTrue(credentials.contains("dbadmin"), "Username should be in credentials section");
        assertTrue(credentials.contains("s3cr3tPass"), "Password should be in credentials section");
        assertTrue(credentials.contains(":"), "Credentials should have username:password format");
        
        assertEquals("aws-0-eu-west-1.pooler.supabase.com:6543", hostPort,
            "Host and port should be correctly positioned after credentials");
    }

    @Test
    @DisplayName("Enhanced: PostgreSQL URL format credential extraction to proper jdbc:postgresql://")
    void testPostgresqlSchemeCredentialExtractionToProperJdbcFormat() {
        String postgresqlUrl = "postgresql://app_user:C0mpl3x!P@ss@db.proj123xyz456.supabase.co:5432/appdb?connect_timeout=10";
        environment.getSystemProperties().put("DATABASE_URL", postgresqlUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl, "JDBC URL must be generated from postgresql:// format");
        assertEquals("jdbc:postgresql://app_user:C0mpl3x!P@ss@db.proj123xyz456.supabase.co:5432/appdb?connect_timeout=10",
            jdbcUrl, "postgresql:// scheme must be converted with credentials intact");
        
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"),
            "Must start with jdbc:postgresql:// prefix");
        assertFalse(jdbcUrl.startsWith("postgresql://"),
            "Must not retain original postgresql:// scheme");
        
        assertTrue(jdbcUrl.contains("app_user:C0mpl3x!P@ss@"),
            "Credentials with special characters must be preserved exactly");
        assertTrue(jdbcUrl.contains("@db.proj123xyz456.supabase.co:5432/appdb"),
            "Host, port, and database must follow credentials");
        assertTrue(jdbcUrl.endsWith("?connect_timeout=10"),
            "Query parameters must be preserved");
    }

    @Test
    @DisplayName("Enhanced: Spring property resolution returns JDBC URL not unconverted DATABASE_URL")
    void testSpringPropertyResolutionReturnsJdbcNotUnconverted() {
        String unconvertedDatabaseUrl = "postgres://resolver:resolverpass@resolver.example.supabase.co:5432/resolverdb";
        String expectedJdbcUrl = "jdbc:postgresql://resolver:resolverpass@resolver.example.supabase.co:5432/resolverdb";
        
        environment.getSystemProperties().put("DATABASE_URL", unconvertedDatabaseUrl);
        
        Map<String, Object> props = new HashMap<>();
        props.put("some.unrelated.property", "unrelated-value");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("unrelatedProperties", props)
        );
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedDatasourceUrl = environment.getProperty("spring.datasource.url");
        String rawDatabaseUrl = environment.getProperty("DATABASE_URL");
        
        assertNotNull(resolvedDatasourceUrl,
            "Spring must be able to resolve spring.datasource.url");
        assertEquals(expectedJdbcUrl, resolvedDatasourceUrl,
            "Spring property resolution must return converted JDBC format");
        assertNotEquals(unconvertedDatabaseUrl, resolvedDatasourceUrl,
            "Spring must NOT return unconverted postgres:// DATABASE_URL value");
        
        assertEquals(unconvertedDatabaseUrl, rawDatabaseUrl,
            "DATABASE_URL property should remain in original format");
        
        assertTrue(resolvedDatasourceUrl.startsWith("jdbc:"),
            "Datasource URL must be JDBC-formatted for DataSource initialization");
        assertTrue(rawDatabaseUrl.startsWith("postgres://"),
            "DATABASE_URL should remain unconverted in environment");
        
        assertFalse(resolvedDatasourceUrl.equals(rawDatabaseUrl),
            "The two properties must differ, proving conversion occurred");
    }

    @Test
    @DisplayName("Enhanced: Property source ordering - EnvironmentPostProcessor added properties override defaults")
    void testPropertySourceOrderingEnvironmentPostProcessorOverridesDefaults() {
        String envDatabaseUrl = "postgres://override:overridepass@override.example.com:5432/overridedb";
        String expectedJdbcUrl = "jdbc:postgresql://override:overridepass@override.example.com:5432/overridedb";
        
        Map<String, Object> defaultProperties = new HashMap<>();
        defaultProperties.put("spring.datasource.url", "jdbc:h2:mem:testdb");
        defaultProperties.put("spring.datasource.driver-class-name", "org.h2.Driver");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("defaultProperties", defaultProperties)
        );
        
        Map<String, Object> applicationProperties = new HashMap<>();
        applicationProperties.put("spring.datasource.url", "jdbc:postgresql://default:defaultpass@default.host:5432/defaultdb");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("applicationConfig", applicationProperties)
        );
        
        environment.getSystemProperties().put("DATABASE_URL", envDatabaseUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertEquals(expectedJdbcUrl, resolvedUrl,
            "EnvironmentPostProcessor properties must override all default and application properties");
        assertNotEquals("jdbc:h2:mem:testdb", resolvedUrl,
            "Default H2 database should be overridden");
        assertNotEquals("jdbc:postgresql://default:defaultpass@default.host:5432/defaultdb", resolvedUrl,
            "Application config default should be overridden");
        
        org.springframework.core.env.PropertySource<?> firstPropertySource = 
            environment.getPropertySources().iterator().next();
        assertEquals("databaseUrlConversion", firstPropertySource.getName(),
            "DatabaseUrlConverter property source must be first in the chain");
        
        Object valueFromFirst = firstPropertySource.getProperty("spring.datasource.url");
        assertEquals(expectedJdbcUrl, valueFromFirst,
            "First property source must contain the converted URL");
    }

    @Test
    @DisplayName("Enhanced: Credential extraction before Spring datasource hostname parsing")
    void testCredentialExtractionBeforeSpringDatasourceHostnameParsing() {
        String urlWithComplexCredentials = "postgres://complex.user:p@ssw0rd:with:3:colons:here@db.test.supabase.co:5432/testdb";
        environment.getSystemProperties().put("DATABASE_URL", urlWithComplexCredentials);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "Converted URL must exist");
        
        String expectedUrl = "jdbc:postgresql://complex.user:p@ssw0rd:with:3:colons:here@db.test.supabase.co:5432/testdb";
        assertEquals(expectedUrl, convertedUrl,
            "Credentials with multiple colons must be preserved correctly");
        
        int schemeEndIndex = convertedUrl.indexOf("://") + 3;
        String afterScheme = convertedUrl.substring(schemeEndIndex);
        int finalAtIndex = afterScheme.lastIndexOf("@");
        
        assertTrue(finalAtIndex > 0, "URL must contain @ separator");
        
        String credentialsPart = afterScheme.substring(0, finalAtIndex);
        String hostPart = afterScheme.substring(finalAtIndex + 1);
        
        assertEquals("complex.user:p@ssw0rd:with:3:colons:here", credentialsPart,
            "All credential content including colons must be before final @");
        
        assertTrue(hostPart.startsWith("db.test.supabase.co"),
            "Hostname must start immediately after final @");
        assertFalse(hostPart.contains("complex.user"),
            "Hostname section must not contain username");
        assertFalse(hostPart.contains("p@ssw0rd"),
            "Hostname section must not contain password");
        
        String hostPortDb = hostPart.split("\\?")[0];
        assertEquals("db.test.supabase.co:5432/testdb", hostPortDb,
            "Host, port, and database must be correctly extracted without credential interference");
    }

    @Test
    @DisplayName("Enhanced: Property source precedence ensures converter properties are highest priority")
    void testPropertySourcePrecedenceConverterPropertiesHighestPriority() {
        String postgresUrl = "postgres://highest:highestpass@highest.priority.com:5432/highestdb";
        String expectedJdbc = "jdbc:postgresql://highest:highestpass@highest.priority.com:5432/highestdb";
        
        Map<String, Object> systemEnvSimulation = new HashMap<>();
        systemEnvSimulation.put("spring.datasource.url", "jdbc:postgresql://system:systempass@system.host:5432/systemdb");
        environment.getPropertySources().addFirst(
            new org.springframework.core.env.MapPropertySource("systemEnvironment", systemEnvSimulation)
        );
        
        Map<String, Object> commandLineArgs = new HashMap<>();
        commandLineArgs.put("spring.datasource.url", "jdbc:postgresql://cmdline:cmdlinepass@cmdline.host:5432/cmdlinedb");
        environment.getPropertySources().addFirst(
            new org.springframework.core.env.MapPropertySource("commandLineArgs", commandLineArgs)
        );
        
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        int initialSize = environment.getPropertySources().size();
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        int afterConversionSize = environment.getPropertySources().size();
        assertEquals(initialSize + 1, afterConversionSize,
            "Converter should add exactly one new property source");
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertEquals(expectedJdbc, resolvedUrl,
            "Converter-added property must override even commandLineArgs");
        
        org.springframework.core.env.PropertySource<?> firstSource = 
            environment.getPropertySources().iterator().next();
        assertEquals("databaseUrlConversion", firstSource.getName(),
            "Converter property source must be absolute first");
        
        Object firstSourceValue = firstSource.getProperty("spring.datasource.url");
        assertEquals(expectedJdbc, firstSourceValue,
            "First property source must contain converted JDBC URL");
    }

    @Test
    @DisplayName("Enhanced: Credentials with multiple @ symbols properly handled")
    void testCredentialsWithMultipleAtSymbolsProperlyHandled() {
        String urlWithMultipleAts = "postgres://user@@:pass@@word@@@db.multi.supabase.co:5432/multidb";
        environment.getSystemProperties().put("DATABASE_URL", urlWithMultipleAts);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "URL with multiple @ symbols should be converted");
        assertEquals("jdbc:postgresql://user@@:pass@@word@@@db.multi.supabase.co:5432/multidb",
            convertedUrl, "Multiple @ symbols in credentials should be preserved");
        
        String afterScheme = convertedUrl.substring("jdbc:postgresql://".length());
        int lastAtIndex = afterScheme.lastIndexOf("@");
        String hostSection = afterScheme.substring(lastAtIndex + 1);
        
        assertTrue(hostSection.startsWith("db.multi.supabase.co"),
            "Hostname should be identified correctly after multiple @ symbols");
    }

    @Test
    @DisplayName("Enhanced: Verify application.properties DATABASE_URL fallback is bypassed by converter")
    void testApplicationPropertiesDatabaseUrlFallbackBypassedByConverter() {
        String envDatabaseUrl = "postgres://bypass:bypasspass@bypass.host.co:5432/bypassdb?sslmode=require";
        String expectedJdbcUrl = "jdbc:postgresql://bypass:bypasspass@bypass.host.co:5432/bypassdb?sslmode=require";
        
        Map<String, Object> applicationConfig = new HashMap<>();
        applicationConfig.put("spring.datasource.url", "${spring.datasource.url:${DATABASE_URL:jdbc:h2:mem:testdb}}");
        environment.getPropertySources().addLast(
            new org.springframework.core.env.MapPropertySource("applicationConfig", applicationConfig)
        );
        
        environment.getSystemProperties().put("DATABASE_URL", envDatabaseUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(resolvedUrl, "URL must be resolved");
        assertEquals(expectedJdbcUrl, resolvedUrl,
            "Converter must bypass fallback chain and provide JDBC URL directly");
        assertNotEquals(envDatabaseUrl, resolvedUrl,
            "Must not resolve to raw DATABASE_URL from fallback");
        assertNotEquals("jdbc:h2:mem:testdb", resolvedUrl,
            "Must not fall through to H2 default");
        
        assertTrue(resolvedUrl.startsWith("jdbc:postgresql://"),
            "Must be converted JDBC PostgreSQL format");
        assertFalse(resolvedUrl.contains("${"),
            "Must not contain unresolved placeholders");
    }

    @Test
    @DisplayName("Enhanced: Complex password with special characters preserved in credential extraction")
    void testComplexPasswordWithSpecialCharactersInCredentialExtraction() {
        String complexPasswordUrl = "postgres://admin:P@$$w0rd!#%^&*()_+-=[]{}|;:',.<>?/~`@db.complex.supabase.co:5432/complexdb";
        environment.getSystemProperties().put("DATABASE_URL", complexPasswordUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String convertedUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(convertedUrl, "Complex password URL should be converted");
        
        String expectedUrl = "jdbc:postgresql://admin:P@$$w0rd!#%^&*()_+-=[]{}|;:',.<>?/~`@db.complex.supabase.co:5432/complexdb";
        assertEquals(expectedUrl, convertedUrl,
            "All special characters in password must be preserved");
        
        assertTrue(convertedUrl.contains("admin:P@$$w0rd!#%^&*()_+-=[]{}|;:',.<>?/~`@"),
            "Complete complex password must be in credentials section");
        
        String afterScheme = convertedUrl.substring("jdbc:postgresql://".length());
        int lastAt = afterScheme.lastIndexOf("@");
        String hostSection = afterScheme.substring(lastAt + 1);
        
        assertEquals("db.complex.supabase.co:5432/complexdb", hostSection,
            "Host section must be correctly identified despite special characters in password");
    }

    @Test
    @DisplayName("Enhanced: Verify conversion takes precedence even with existing spring.datasource.url in environment")
    void testConversionTakesPrecedenceWithExistingDatasourceUrl() {
        String existingJdbcUrl = "jdbc:postgresql://existing:existingpass@existing.host:5432/existingdb";
        Map<String, Object> existingProperties = new HashMap<>();
        existingProperties.put("spring.datasource.url", existingJdbcUrl);
        environment.getPropertySources().addFirst(
            new org.springframework.core.env.MapPropertySource("existingConfig", existingProperties)
        );
        
        String newDatabaseUrl = "postgres://new:newpass@new.host.supabase.co:5432/newdb";
        String expectedNewJdbcUrl = "jdbc:postgresql://new:newpass@new.host.supabase.co:5432/newdb";
        environment.getSystemProperties().put("DATABASE_URL", newDatabaseUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resolvedUrl = environment.getProperty("spring.datasource.url");
        assertEquals(expectedNewJdbcUrl, resolvedUrl,
            "Newly converted URL must override pre-existing spring.datasource.url");
        assertNotEquals(existingJdbcUrl, resolvedUrl,
            "Existing JDBC URL should be overridden");
        
        org.springframework.core.env.PropertySource<?> firstSource = 
            environment.getPropertySources().iterator().next();
        assertEquals("databaseUrlConversion", firstSource.getName(),
            "Converter property source must be first, overriding existing config");
    }
}
