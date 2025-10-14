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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseUrlConverter Integration Tests")
class DatabaseUrlConverterIntegrationTest {

    private DatabaseUrlConverter converter;
    private ConfigurableEnvironment environment;

    @BeforeEach
    void setUp() {
        converter = new DatabaseUrlConverter();
        environment = new StandardEnvironment();
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
    @DisplayName("Should not modify already JDBC formatted URLs")
    void testDoesNotModifyJdbcUrls() {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/testdb";
        environment.getSystemProperties().put("DATABASE_URL", jdbcUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resultUrl = environment.getProperty("spring.datasource.url");
        assertNull(resultUrl);
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
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
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
        assertNull(resultUrl);
        
        boolean foundLogMessage = listAppender.list.stream()
            .anyMatch(event -> 
                event.getMessage().contains("Database URL is already in JDBC format") &&
                event.getMessage().contains("no conversion performed") &&
                event.getFormattedMessage().contains(jdbcUrl));
        
        assertTrue(foundLogMessage, "Expected log message about JDBC format detection was not found");
        
        logger.detachAppender(listAppender);
    }
}
