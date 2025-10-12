package it.odvsicilia.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseUrlConverter Tests")
class DatabaseUrlConverterTest {

    @BeforeEach
    void setUp() {
        System.clearProperty("DATABASE_URL");
    }

    @Test
    @DisplayName("Should convert postgres:// scheme to jdbc:postgresql://")
    void testConvertPostgresScheme() {
        String postgresUrl = "postgres://user:pass@localhost:5432/testdb";
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://user:pass@localhost:5432/testdb", jdbcUrl);
    }

    @Test
    @DisplayName("Should convert postgresql:// scheme to jdbc:postgresql://")
    void testConvertPostgresqlScheme() {
        String postgresUrl = "postgresql://user:pass@localhost:5432/testdb";
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertEquals("jdbc:postgresql://user:pass@localhost:5432/testdb", jdbcUrl);
    }

    @Test
    @DisplayName("Should preserve host, port, and database name")
    void testPreserveHostPortDatabase() {
        String postgresUrl = "postgres://user:pass@db.example.com:5433/mydatabase";
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("db.example.com"));
        assertTrue(jdbcUrl.contains(":5433/"));
        assertTrue(jdbcUrl.contains("/mydatabase"));
    }

    @Test
    @DisplayName("Should preserve query parameters")
    void testPreserveQueryParameters() {
        String postgresUrl = "postgres://user:pass@localhost:5432/testdb?sslmode=require&connect_timeout=10";
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("?sslmode=require&connect_timeout=10"));
    }

    @Test
    @DisplayName("Should preserve credentials in URL")
    void testPreserveCredentials() {
        String postgresUrl = "postgres://myuser:mypassword@localhost:5432/testdb";
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", postgresUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("myuser:mypassword@"));
    }

    @Test
    @DisplayName("Should not modify URL without postgres:// or postgresql:// scheme")
    void testDoNotModifyOtherSchemes() {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/testdb";
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", jdbcUrl);
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String resultUrl = environment.getProperty("spring.datasource.url");
        assertNull(resultUrl);
    }

    @Test
    @DisplayName("Should handle empty DATABASE_URL")
    void testHandleEmptyDatabaseUrl() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", "");
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNull(jdbcUrl);
    }

    @Test
    @DisplayName("Should handle null DATABASE_URL")
    void testHandleNullDatabaseUrl() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        
        ConfigurableEnvironment environment = new StandardEnvironment();
        String existingUrl = environment.getProperty("spring.datasource.url");
        
        converter.postProcessEnvironment(environment, new SpringApplication());
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertEquals(existingUrl, jdbcUrl);
    }
}
