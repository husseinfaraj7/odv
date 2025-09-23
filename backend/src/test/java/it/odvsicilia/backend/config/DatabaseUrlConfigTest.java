package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DatabaseUrlConfigTest {

    private DatabaseUrlConfig databaseUrlConfig;
    private MockEnvironment environment;

    @BeforeEach
    public void setUp() {
        environment = new MockEnvironment();
        databaseUrlConfig = new DatabaseUrlConfig(environment);
    }

    @Test
    public void testTransformDatabaseUrl_WithoutJdbcPrefix() {
        // Given
        environment.setProperty("DATABASE_URL", "postgresql://user:pass@localhost:5432/testdb");
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:postgresql://user:pass@localhost:5432/testdb", result);
    }

    @Test
    public void testTransformDatabaseUrl_WithJdbcPrefix() {
        // Given
        environment.setProperty("DATABASE_URL", "jdbc:postgresql://user:pass@localhost:5432/testdb");
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:postgresql://user:pass@localhost:5432/testdb", result);
    }

    @Test
    public void testTransformDatabaseUrl_NullUrl() {
        // Given - no DATABASE_URL set
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:h2:mem:testdb", result);
    }

    @Test
    public void testTransformDatabaseUrl_EmptyUrl() {
        // Given
        environment.setProperty("DATABASE_URL", "");
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:h2:mem:testdb", result);
    }
}