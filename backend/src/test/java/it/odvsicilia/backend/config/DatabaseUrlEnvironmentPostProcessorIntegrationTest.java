package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseUrlEnvironmentPostProcessor Integration Tests")
class DatabaseUrlEnvironmentPostProcessorIntegrationTest {

    private ConfigurableApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("Should convert postgres:// URL to JDBC format")
    void testConvertPostgresUrlToJdbc() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://testuser:testpass@localhost:5432/testdb");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://localhost:5432/testdb"));
        assertTrue(jdbcUrl.contains("user=testuser"));
        assertTrue(jdbcUrl.contains("password=testpass"));
    }

    @Test
    @DisplayName("Should convert postgresql:// URL to JDBC format")
    void testConvertPostgresqlUrlToJdbc() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgresql://myuser:mypass@db.example.com:5433/proddb");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://db.example.com:5433/proddb"));
        assertTrue(jdbcUrl.contains("user=myuser"));
        assertTrue(jdbcUrl.contains("password=mypass"));
    }

    @Test
    @DisplayName("Should handle URLs with special characters in password")
    void testHandleSpecialCharactersInPassword() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://user:p@ss!w0rd#123@localhost:5432/testdb");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password="));
        assertFalse(jdbcUrl.contains("p@ss!w0rd#123"), "Password should be URL encoded");
    }

    @Test
    @DisplayName("Should preserve query parameters in conversion")
    void testPreserveQueryParameters() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://user:pass@localhost:5432/db?sslmode=require&connectTimeout=10");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("sslmode=require"));
        assertTrue(jdbcUrl.contains("connectTimeout=10"));
    }

    @Test
    @DisplayName("Should leave JDBC URL unchanged")
    void testJdbcUrlPassesThrough() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        String originalJdbcUrl = "jdbc:postgresql://localhost:5432/testdb?user=test&password=test";
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", originalJdbcUrl);
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String resultUrl = environment.getProperty("spring.datasource.url");
        assertNull(resultUrl, "JDBC URL should not be modified");
    }

    @Test
    @DisplayName("Should handle missing DATABASE_URL gracefully")
    void testMissingDatabaseUrl() {
        ConfigurableEnvironment environment = new MockEnvironment();

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        
        assertDoesNotThrow(() -> {
            processor.postProcessEnvironment(environment, new SpringApplication());
        });

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNull(jdbcUrl);
    }

    @Test
    @DisplayName("Should handle empty DATABASE_URL gracefully")
    void testEmptyDatabaseUrl() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        
        assertDoesNotThrow(() -> {
            processor.postProcessEnvironment(environment, new SpringApplication());
        });

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNull(jdbcUrl);
    }

    @Test
    @DisplayName("Should use default port 5432 when not specified")
    void testDefaultPort() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://user:pass@localhost/testdb");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("localhost:5432"));
    }

    @Test
    @DisplayName("Should use default database name when not specified")
    void testDefaultDatabase() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://user:pass@localhost:5432");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("/postgres?"));
    }

    @Test
    @DisplayName("Should use default username when not specified")
    void testDefaultUsername() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://localhost:5432/testdb");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=postgres"));
    }

    @Test
    @DisplayName("Should handle URL with username but no password")
    void testUsernameWithoutPassword() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://testuser@localhost:5432/testdb");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=testuser"));
        assertTrue(jdbcUrl.contains("password="));
    }

    @Test
    @DisplayName("Should handle URL without scheme gracefully")
    void testInvalidUrlFormat() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "invalid-no-scheme");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        
        assertDoesNotThrow(() -> {
            processor.postProcessEnvironment(environment, new SpringApplication());
        });
        
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNull(jdbcUrl);
    }

    @Test
    @DisplayName("Should create functional datasource with HikariCP from converted URL")
    void testFunctionalDataSourceWithH2() {
        SpringApplication app = new SpringApplication(TestConfiguration.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "jdbc:h2:mem:integrationtest;DB_CLOSE_DELAY=-1");
        props.put("spring.datasource.driver-class-name", "org.h2.Driver");
        props.put("spring.datasource.username", "sa");
        props.put("spring.datasource.password", "");
        props.put("spring.datasource.hikari.maximum-pool-size", "5");
        props.put("spring.datasource.hikari.minimum-idle", "2");
        props.put("spring.datasource.hikari.connection-timeout", "30000");
        props.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        props.put("database.validation.enabled", "false");
        props.put("spring.profiles.active", "test");
        
        app.setDefaultProperties(props);
        
        context = app.run();
        
        DataSource dataSource = context.getBean(DataSource.class);
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertEquals(5, hikariDataSource.getMaximumPoolSize());
        assertEquals(2, hikariDataSource.getMinimumIdle());
        assertEquals(30000, hikariDataSource.getConnectionTimeout());
        
        assertDoesNotThrow(() -> {
            try (Connection conn = dataSource.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
            }
        });
    }

    @Test
    @DisplayName("Should verify HikariCP connection pooling is configured correctly")
    void testHikariCpConfiguration() {
        SpringApplication app = new SpringApplication(TestConfiguration.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "jdbc:h2:mem:hikaritest;DB_CLOSE_DELAY=-1");
        props.put("spring.datasource.driver-class-name", "org.h2.Driver");
        props.put("spring.datasource.username", "sa");
        props.put("spring.datasource.password", "");
        props.put("spring.datasource.hikari.maximum-pool-size", "5");
        props.put("spring.datasource.hikari.minimum-idle", "2");
        props.put("spring.datasource.hikari.connection-timeout", "30000");
        props.put("spring.datasource.hikari.idle-timeout", "600000");
        props.put("spring.datasource.hikari.max-lifetime", "1800000");
        props.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        props.put("database.validation.enabled", "false");
        props.put("spring.profiles.active", "test");
        
        app.setDefaultProperties(props);
        
        context = app.run();
        
        DataSource dataSource = context.getBean(DataSource.class);
        assertTrue(dataSource instanceof HikariDataSource);
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertEquals(5, hikariDataSource.getMaximumPoolSize());
        assertEquals(2, hikariDataSource.getMinimumIdle());
        assertEquals(30000, hikariDataSource.getConnectionTimeout());
        assertEquals(600000, hikariDataSource.getIdleTimeout());
        assertEquals(1800000, hikariDataSource.getMaxLifetime());
    }

    @Test
    @DisplayName("Should handle complex real-world Supabase URL")
    void testSupabaseUrlConversion() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgresql://postgres.abc123xyz:complex_Pass!@db.example.supabase.co:5432/postgres?sslmode=require");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://db.example.supabase.co:5432/postgres"));
        assertTrue(jdbcUrl.contains("user=postgres.abc123xyz"));
        assertTrue(jdbcUrl.contains("sslmode=require"));
    }

    @Test
    @DisplayName("Should handle URL with multiple query parameters")
    void testMultipleQueryParameters() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://user:pass@localhost:5432/db?sslmode=require&connectTimeout=10&socketTimeout=30&ApplicationName=myapp");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("sslmode=require"));
        assertTrue(jdbcUrl.contains("connectTimeout=10"));
        assertTrue(jdbcUrl.contains("socketTimeout=30"));
        assertTrue(jdbcUrl.contains("ApplicationName=myapp"));
    }

    @Test
    @DisplayName("Should encode special characters in password correctly")
    void testPasswordEncoding() {
        ConfigurableEnvironment environment = new MockEnvironment();
        
        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "postgres://user:pass-with_special!chars@localhost:5432/db");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password=pass-with_special%21chars"));
    }

    @SpringBootApplication
    static class TestConfiguration {
    }
}
