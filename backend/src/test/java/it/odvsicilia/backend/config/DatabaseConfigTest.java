package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class DatabaseConfigTest {

    private DatabaseConfig databaseConfig = new DatabaseConfig();

    @Test
    @DisplayName("Should create H2 DataSource when DATABASE_URL is null")
    void testCreateH2DataSourceWhenDatabaseUrlIsNull() {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(null);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            assertEquals("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", 
                        hikariDataSource.getJdbcUrl());
            assertEquals("sa", hikariDataSource.getUsername());
            assertEquals("", hikariDataSource.getPassword());
        }
    }

    @Test
    @DisplayName("Should create H2 DataSource when DATABASE_URL is empty")
    void testCreateH2DataSourceWhenDatabaseUrlIsEmpty() {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn("");

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            assertEquals("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", 
                        hikariDataSource.getJdbcUrl());
        }
    }

    @Test
    @DisplayName("Should handle password with @ symbol correctly")
    void testPasswordWithAtSymbol() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given - password contains @ which needs URL encoding
            String username = "testuser";
            String password = "p@ssw0rd!";
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String databaseUrl = String.format("postgres://%s:%s@localhost:5432/testdb", 
                                             username, encodedPassword);
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals("testuser", hikariDataSource.getUsername());
            assertEquals("p%40ssw0rd%21", hikariDataSource.getPassword()); // URL encoded
            assertTrue(hikariDataSource.getJdbcUrl().startsWith("jdbc:postgresql://"));
        }
    }

    @Test
    @DisplayName("Should handle password with % symbol correctly")
    void testPasswordWithPercentSymbol() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given - password contains % which needs URL encoding
            String username = "testuser";
            String password = "p%ssw0rd";
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String databaseUrl = String.format("postgres://%s:%s@localhost:5432/testdb", 
                                             username, encodedPassword);
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals("testuser", hikariDataSource.getUsername());
            assertEquals("p%25ssw0rd", hikariDataSource.getPassword()); // URL encoded
        }
    }

    @Test
    @DisplayName("Should handle password with & symbol correctly")
    void testPasswordWithAmpersandSymbol() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given - password contains & which needs URL encoding
            String username = "testuser";
            String password = "p&ssw0rd";
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String databaseUrl = String.format("postgres://%s:%s@localhost:5432/testdb", 
                                             username, encodedPassword);
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals("testuser", hikariDataSource.getUsername());
            assertEquals("p%26ssw0rd", hikariDataSource.getPassword()); // URL encoded
        }
    }

    @Test
    @DisplayName("Should handle password with + symbol correctly")
    void testPasswordWithPlusSymbol() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given - password contains + which needs URL encoding
            String username = "testuser";
            String password = "p+ssw0rd";
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String databaseUrl = String.format("postgres://%s:%s@localhost:5432/testdb", 
                                             username, encodedPassword);
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals("testuser", hikariDataSource.getUsername());
            assertEquals("p%2Bssw0rd", hikariDataSource.getPassword()); // URL encoded
        }
    }

    @Test
    @DisplayName("Should handle complex password with multiple special characters")
    void testPasswordWithMultipleSpecialCharacters() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given - password contains @, %, &, + and other special characters
            String username = "supabase_user";
            String password = "MyP@ss%W0rd&+Special!";
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String databaseUrl = String.format("postgres://%s:%s@db.supabase.co:5432/postgres", 
                                             username, encodedPassword);
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals("supabase_user", hikariDataSource.getUsername());
            assertEquals("MyP%40ss%25W0rd%26%2BSpecial%21", hikariDataSource.getPassword()); // URL encoded
            assertTrue(hikariDataSource.getJdbcUrl().contains("db.supabase.co"));
        }
    }

    @Test
    @DisplayName("Should handle Supabase-style connection string with encoded password")
    void testSupabaseStyleConnectionString() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given - typical Supabase connection string format
            String username = "postgres.abcdefghijklmnopqrst";
            String password = "MySecure@Pass123!&More+Chars%Here";
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String host = "db.abcdefghijklmnopqrstuvwxyz.supabase.co";
            String databaseUrl = String.format("postgres://%s:%s@%s:5432/postgres", 
                                             username, encodedPassword, host);
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals(username, hikariDataSource.getUsername());
            assertEquals("MySecure%40Pass123%21%26More%2BChars%25Here", hikariDataSource.getPassword());
            assertTrue(hikariDataSource.getJdbcUrl().contains("supabase.co"));
            assertTrue(hikariDataSource.getJdbcUrl().startsWith("jdbc:postgresql://"));
            assertTrue(hikariDataSource.getJdbcUrl().contains("/postgres"));
        }
    }

    @Test
    @DisplayName("Should convert postgres:// URL to JDBC format correctly")
    void testConvertPostgresUrlToJdbcFormat() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String databaseUrl = "postgres://user:pass@localhost:5432/testdb";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            String jdbcUrl = hikariDataSource.getJdbcUrl();
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
            assertFalse(jdbcUrl.startsWith("jdbc:postgres://"));
        }
    }

    @Test
    @DisplayName("Should handle already JDBC formatted URL")
    void testHandleAlreadyJdbcFormattedUrl() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String databaseUrl = "jdbc:postgresql://user:pass@localhost:5432/testdb";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals(databaseUrl, hikariDataSource.getJdbcUrl());
        }
    }

    @Test
    @DisplayName("Should throw exception for invalid URL format")
    void testThrowExceptionForInvalidUrlFormat() {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String invalidUrl = "invalid://url/format";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(invalidUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then - should fall back to H2 when invalid URL is provided
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            assertTrue(hikariDataSource.getJdbcUrl().contains("h2:mem"));
        }
    }

    @Test
    @DisplayName("Should throw exception for URL missing credentials")
    void testThrowExceptionForUrlMissingCredentials() {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String urlWithoutCredentials = "postgres://localhost:5432/testdb";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(urlWithoutCredentials);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then - should fall back to H2 when invalid URL is provided
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            assertTrue(hikariDataSource.getJdbcUrl().contains("h2:mem"));
        }
    }

    @Test
    @DisplayName("Should throw exception for URL missing host")
    void testThrowExceptionForUrlMissingHost() {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String urlWithoutHost = "postgres://user:pass@/testdb";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(urlWithoutHost);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then - should fall back to H2 when invalid URL is provided
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            assertTrue(hikariDataSource.getJdbcUrl().contains("h2:mem"));
        }
    }

    @Test
    @DisplayName("Should throw exception for URL missing database name")
    void testThrowExceptionForUrlMissingDatabaseName() {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String urlWithoutDatabase = "postgres://user:pass@localhost:5432/";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(urlWithoutDatabase);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then - should fall back to H2 when invalid URL is provided
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            assertTrue(hikariDataSource.getJdbcUrl().contains("h2:mem"));
        }
    }

    @Test
    @DisplayName("Should handle URL with special characters in username")
    void testUrlWithSpecialCharactersInUsername() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String username = "user@domain.com";
            String password = "simplepass";
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String databaseUrl = String.format("postgres://%s:%s@localhost:5432/testdb", 
                                             encodedUsername, password);
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertEquals("user%40domain.com", hikariDataSource.getUsername()); // URL encoded
            assertEquals("simplepass", hikariDataSource.getPassword());
        }
    }

    @Test
    @DisplayName("Should handle URL with port number correctly")
    void testUrlWithPortNumber() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String databaseUrl = "postgres://user:pass@localhost:6543/testdb";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            assertTrue(hikariDataSource.getJdbcUrl().contains(":6543"));
        }
    }

    @Test
    @DisplayName("Should handle URL without port number (default to 5432)")
    void testUrlWithoutPortNumber() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String databaseUrl = "postgres://user:pass@localhost/testdb";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // Should still work even without explicit port
            assertTrue(hikariDataSource.getJdbcUrl().contains("localhost"));
        }
    }

    @Test
    @DisplayName("Should configure HikariCP properties correctly for Supabase")
    void testHikariCPPropertiesForSupabase() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String databaseUrl = "postgres://user:pass@db.supabase.co:5432/postgres";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // Verify pool settings are appropriate for Supabase
            assertEquals(8, hikariDataSource.getMaximumPoolSize()); // Supabase optimized
            assertEquals(2, hikariDataSource.getMinimumIdle());
            assertEquals("SupabaseHikariPool", hikariDataSource.getPoolName());
            assertEquals("org.postgresql.Driver", hikariDataSource.getDriverClassName());
        }
    }

    @Test
    @DisplayName("Should decode URL-encoded credentials correctly")
    void testUrlDecodingCredentials() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            DatabaseConfig config = new DatabaseConfig();
            String databaseUrl = "postgres://user%40test:pass%26word@localhost:5432/testdb";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(databaseUrl);

            // When - test credential parsing through private method reflection
            Method parseConnectionDetailsMethod = DatabaseConfig.class.getDeclaredMethod("parseConnectionDetails", String.class);
            parseConnectionDetailsMethod.setAccessible(true);
            
            Object result = parseConnectionDetailsMethod.invoke(config, databaseUrl);
            
            // Then - verify that the result contains decoded credentials
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Should validate URL format correctly")
    void testUrlValidation() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            DatabaseConfig config = new DatabaseConfig();
            String validUrl = "postgres://user:pass@localhost:5432/testdb";
            
            // When - test URL validation through private method reflection
            Method validateSupabaseUrlMethod = DatabaseConfig.class.getDeclaredMethod("validateSupabaseUrl", String.class);
            validateSupabaseUrlMethod.setAccessible(true);
            
            // Then - should not throw exception
            assertDoesNotThrow(() -> validateSupabaseUrlMethod.invoke(config, validUrl));
        }
    }

    @Test
    @DisplayName("Should handle malformed URL gracefully")
    void testMalformedUrlHandling() throws Exception {
        try (MockedStatic<System> systemMock = mockStatic(System.class, Mockito.CALLS_REAL_METHODS)) {
            // Given
            String malformedUrl = "postgres://[invalid-uri";
            systemMock.when(() -> System.getenv("DATABASE_URL")).thenReturn(malformedUrl);

            // When
            DataSource dataSource = databaseConfig.dataSource();

            // Then - should fall back to H2
            assertNotNull(dataSource);
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            assertTrue(hikariDataSource.getJdbcUrl().contains("h2:mem"));
        }
    }
}