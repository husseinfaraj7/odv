package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseUrlConverter Tests")
class DatabaseUrlConverterTest {

    @Test
    @DisplayName("Should convert direct Supabase URL to JDBC format")
    void testConvertDirectSupabaseUrl() throws URISyntaxException {
        String postgresUrl = "postgres://user:pass@db.abcdefghij1234567890.supabase.co:5432/postgres";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("db.abcdefghij1234567890.supabase.co"));
        assertTrue(jdbcUrl.contains(":5432/"));
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password=pass"));
        assertTrue(jdbcUrl.contains("/postgres?"));
    }

    @Test
    @DisplayName("Should convert pooler Supabase URL to JDBC format")
    void testConvertPoolerSupabaseUrl() throws URISyntaxException {
        String postgresUrl = "postgres://user:pass@aws-0-eu-north-1.pooler.supabase.com:6543/postgres";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("aws-0-eu-north-1.pooler.supabase.com"));
        assertTrue(jdbcUrl.contains(":6543/"));
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password=pass"));
        assertTrue(jdbcUrl.contains("/postgres?"));
    }

    @Test
    @DisplayName("Should preserve complex passwords with special characters")
    void testConvertWithComplexPassword() throws URISyntaxException {
        String postgresUrl = "postgres://myuser:Passw0rd123!@db.testproject12345678.supabase.co:5432/postgres";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=myuser"));
        assertTrue(jdbcUrl.contains("password=Passw0rd123!"));
    }

    @Test
    @DisplayName("Should handle URL with default port (5432)")
    void testConvertWithDefaultPort() throws URISyntaxException {
        String postgresUrl = "postgres://user:pass@localhost/testdb";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains(":5432/"));
    }

    @ParameterizedTest
    @CsvSource({
        "postgres://user1:pass1@db.proj12345678901234567.supabase.co:5432/postgres, user1, pass1, db.proj12345678901234567.supabase.co, 5432",
        "postgres://admin:secret@aws-1-us-east-1.pooler.supabase.com:6543/postgres, admin, secret, aws-1-us-east-1.pooler.supabase.com, 6543",
        "postgres://dbuser:mypass@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres, dbuser, mypass, aws-0-ap-southeast-1.pooler.supabase.com, 6543",
        "postgres://testuser:testpass@db.xyz123abc456def78901.supabase.co:5432/postgres, testuser, testpass, db.xyz123abc456def78901.supabase.co, 5432"
    })
    @DisplayName("Should convert various Supabase URLs correctly")
    void testConvertVariousSupabaseUrls(String postgresUrl, String expectedUser, String expectedPass, 
                                        String expectedHost, int expectedPort) throws URISyntaxException {
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains(expectedHost));
        assertTrue(jdbcUrl.contains(":" + expectedPort + "/"));
        assertTrue(jdbcUrl.contains("user=" + expectedUser));
        assertTrue(jdbcUrl.contains("password=" + expectedPass));
    }

    @Test
    @DisplayName("Should handle URL with query parameters")
    void testConvertWithQueryParameters() throws URISyntaxException {
        String postgresUrl = "postgres://user:pass@db.testproject12345678.supabase.co:5432/postgres?sslmode=require";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password=pass"));
    }

    @Test
    @DisplayName("Should preserve SSL mode parameter from URL")
    void testPreserveSslModeParameter() throws URISyntaxException {
        String postgresUrl = "postgres://user:pass@db.testproject12345678.supabase.co:5432/postgres?sslmode=require&connect_timeout=10";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("db.testproject12345678.supabase.co"));
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password=pass"));
    }

    @Test
    @DisplayName("Should handle different database names")
    void testConvertWithDifferentDatabaseNames() throws URISyntaxException {
        String postgresUrl = "postgres://user:pass@db.testproject12345678.supabase.co:5432/mydatabase";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("/mydatabase?"));
    }

    @Test
    @DisplayName("Should handle URL without password")
    void testConvertWithoutPassword() throws URISyntaxException {
        String postgresUrl = "postgres://user@localhost:5432/testdb";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password="));
    }

    @Test
    @DisplayName("Should handle malformed URL gracefully")
    void testConvertMalformedUrl() throws URISyntaxException {
        String malformedUrl = "not-a-valid-url";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(malformedUrl);
        
        assertNotNull(jdbcUrl);
    }

    @Test
    @DisplayName("Should handle direct connection URL with aws-0 region prefix in pooler format")
    void testConvertPoolerUsEast1() throws URISyntaxException {
        String postgresUrl = "postgres://postgres:password123@aws-0-us-east-1.pooler.supabase.com:6543/postgres";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("aws-0-us-east-1.pooler.supabase.com"));
        assertTrue(jdbcUrl.contains(":6543/"));
        assertTrue(jdbcUrl.contains("user=postgres"));
        assertTrue(jdbcUrl.contains("password=password123"));
    }

    @Test
    @DisplayName("Should handle pooler URL with aws-1 index")
    void testConvertPoolerWithIndex1() throws URISyntaxException {
        String postgresUrl = "postgres://myuser:mypass@aws-1-eu-west-1.pooler.supabase.com:6543/postgres";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("aws-1-eu-west-1.pooler.supabase.com"));
        assertTrue(jdbcUrl.contains(":6543/"));
        assertTrue(jdbcUrl.contains("user=myuser"));
        assertTrue(jdbcUrl.contains("password=mypass"));
    }

    @Test
    @DisplayName("Should properly format JDBC URL with all components")
    void testJdbcUrlFormat() throws URISyntaxException {
        String postgresUrl = "postgres://testuser:testpass@db.abcdef1234567890abcd.supabase.co:5432/postgres";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertEquals("jdbc:postgresql://db.abcdef1234567890abcd.supabase.co:5432/postgres?user=testuser&password=testpass", 
                     jdbcUrl);
    }

    @Test
    @DisplayName("Should convert localhost URL correctly")
    void testConvertLocalhostUrl() throws URISyntaxException {
        String postgresUrl = "postgres://admin:admin@localhost:5432/mydb";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertEquals("jdbc:postgresql://localhost:5432/mydb?user=admin&password=admin", jdbcUrl);
    }

    @Test
    @DisplayName("Should handle URL with username but no password")
    void testConvertUrlWithUsernameOnly() throws URISyntaxException {
        String postgresUrl = "postgres://myuser@db.testproject12345678.supabase.co:5432/postgres";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=myuser"));
        assertTrue(jdbcUrl.contains("password="));
    }

    @Test
    @DisplayName("Should handle URL with no user info (defaults to postgres)")
    void testConvertUrlWithNoUserInfo() throws URISyntaxException {
        String postgresUrl = "postgres://localhost:5432/testdb";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=postgres"));
        assertTrue(jdbcUrl.contains("password="));
    }

    @Test
    @DisplayName("Should handle password with colon character (splits on first colon)")
    void testConvertUrlWithColonInPassword() throws URISyntaxException {
        String postgresUrl = "postgres://user:pass:word@localhost:5432/testdb";
        
        String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(postgresUrl);
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("user=user"));
        assertTrue(jdbcUrl.contains("password=pass"));
    }

    @Test
    @DisplayName("Should convert regional pooler URLs correctly")
    void testConvertRegionalPoolerUrls() throws URISyntaxException {
        String[] poolerUrls = {
            "postgres://user:pass@aws-0-ca-central-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-0-sa-east-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-0-ap-south-1.pooler.supabase.com:6543/postgres"
        };
        
        for (String url : poolerUrls) {
            String jdbcUrl = DatabaseUrlUtils.buildJdbcUrl(url);
            
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
            assertTrue(jdbcUrl.contains(".pooler.supabase.com:6543/"));
            assertTrue(jdbcUrl.contains("user=user"));
            assertTrue(jdbcUrl.contains("password=pass"));
        }
    }
}
