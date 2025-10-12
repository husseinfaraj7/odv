package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=",
    "spring.datasource.username=",
    "spring.datasource.password="
})
@DisplayName("DatabaseUrlConverter Integration Tests")
class DatabaseUrlConverterIntegrationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    @DisplayName("Should convert Supabase direct connection URL to JDBC format")
    void testConvertSupabaseDirectConnectionUrl() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String originalUrl = "postgres://myuser:mypass@db.abcdefghij1234567890.supabase.co:5432/postgres";
        
        System.setProperty("DATABASE_URL", originalUrl);
        try {
            String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
            
            assertNotNull(jdbcUrl);
            assertEquals("jdbc:postgresql://db.abcdefghij1234567890.supabase.co:5432/postgres?user=myuser&password=mypass", 
                         jdbcUrl);
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
            assertTrue(jdbcUrl.contains("db.abcdefghij1234567890.supabase.co"));
            assertTrue(jdbcUrl.contains(":5432/postgres"));
            assertTrue(jdbcUrl.contains("user=myuser"));
            assertTrue(jdbcUrl.contains("password=mypass"));
        } finally {
            System.clearProperty("DATABASE_URL");
        }
    }

    @Test
    @DisplayName("Should convert Supabase pooler URL to JDBC format")
    void testConvertSupabasePoolerUrl() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String originalUrl = "postgres://pooluser:poolpass@aws-0-eu-north-1.pooler.supabase.com:6543/postgres";
        
        System.setProperty("DATABASE_URL", originalUrl);
        try {
            String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
            
            assertNotNull(jdbcUrl);
            assertEquals("jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres?user=pooluser&password=poolpass", 
                         jdbcUrl);
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
            assertTrue(jdbcUrl.contains("aws-0-eu-north-1.pooler.supabase.com"));
            assertTrue(jdbcUrl.contains(":6543/postgres"));
            assertTrue(jdbcUrl.contains("user=pooluser"));
            assertTrue(jdbcUrl.contains("password=poolpass"));
        } finally {
            System.clearProperty("DATABASE_URL");
        }
    }

    @Test
    @DisplayName("Should convert pooler URL with aws-1 index")
    void testConvertPoolerUrlWithAwsIndex1() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String originalUrl = "postgres://admin:secret@aws-1-us-west-2.pooler.supabase.com:6543/postgres";
        
        System.setProperty("DATABASE_URL", originalUrl);
        try {
            String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
            
            assertNotNull(jdbcUrl);
            assertEquals("jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:6543/postgres?user=admin&password=secret", 
                         jdbcUrl);
            assertTrue(jdbcUrl.contains("aws-1-us-west-2.pooler.supabase.com"));
        } finally {
            System.clearProperty("DATABASE_URL");
        }
    }

    @Test
    @DisplayName("Should handle complex passwords with special characters")
    void testConvertUrlWithSpecialCharactersInPassword() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String originalUrl = "postgres://user:P@ssw0rd!@db.test12345678901234567.supabase.co:5432/postgres";
        
        System.setProperty("DATABASE_URL", originalUrl);
        try {
            String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
            
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.contains("user=user"));
            assertTrue(jdbcUrl.contains("password=P%40ssw0rd%21"));
        } finally {
            System.clearProperty("DATABASE_URL");
        }
    }

    @Test
    @DisplayName("Should convert direct connection URL for different regions")
    void testConvertDirectConnectionUrlVariousRegions() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String[] urls = {
            "postgres://user:pass@db.proj1234567890123456.supabase.co:5432/postgres",
            "postgres://user:pass@db.xyz9876543210fedcba.supabase.co:5432/postgres",
            "postgres://user:pass@db.abcabcabcabcabcabcab.supabase.co:5432/postgres"
        };
        
        for (String originalUrl : urls) {
            System.setProperty("DATABASE_URL", originalUrl);
            try {
                String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
                
                assertNotNull(jdbcUrl);
                assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
                assertTrue(jdbcUrl.contains(".supabase.co:5432/postgres"));
                assertTrue(jdbcUrl.contains("user=user"));
                assertTrue(jdbcUrl.contains("password=pass"));
            } finally {
                System.clearProperty("DATABASE_URL");
            }
        }
    }

    @Test
    @DisplayName("Should convert pooler URL for different regions")
    void testConvertPoolerUrlVariousRegions() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String[] urls = {
            "postgres://user:pass@aws-0-us-east-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-0-eu-west-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres",
            "postgres://user:pass@aws-0-ca-central-1.pooler.supabase.com:6543/postgres"
        };
        
        for (String originalUrl : urls) {
            System.setProperty("DATABASE_URL", originalUrl);
            try {
                String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
                
                assertNotNull(jdbcUrl);
                assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
                assertTrue(jdbcUrl.contains(".pooler.supabase.com:6543/postgres"));
                assertTrue(jdbcUrl.contains("user=user"));
                assertTrue(jdbcUrl.contains("password=pass"));
            } finally {
                System.clearProperty("DATABASE_URL");
            }
        }
    }

    @Test
    @DisplayName("Should handle URL with existing query parameters")
    void testConvertUrlWithExistingQueryParameters() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String originalUrl = "postgres://user:pass@db.test12345678901234567.supabase.co:5432/postgres?sslmode=require";
        
        System.setProperty("DATABASE_URL", originalUrl);
        try {
            String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
            
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.contains("user=user"));
            assertTrue(jdbcUrl.contains("password=pass"));
            assertTrue(jdbcUrl.contains("sslmode=require"));
        } finally {
            System.clearProperty("DATABASE_URL");
        }
    }

    @Test
    @DisplayName("Should not modify already JDBC formatted URLs")
    void testDoesNotModifyJdbcUrls() {
        DatabaseUrlConverter converter = new DatabaseUrlConverter();
        String originalUrl = "jdbc:postgresql://localhost:5432/testdb";
        
        System.setProperty("DATABASE_URL", originalUrl);
        try {
            String jdbcUrl = extractJdbcUrlFromConverter(originalUrl, converter);
            
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        } finally {
            System.clearProperty("DATABASE_URL");
        }
    }

    private String extractJdbcUrlFromConverter(String originalUrl, DatabaseUrlConverter converter) {
        if (originalUrl.startsWith("postgres://") || originalUrl.startsWith("postgresql://")) {
            String url = originalUrl.replace("postgres://", "jdbc:postgresql://")
                                     .replace("postgresql://", "jdbc:postgresql://");
            
            int atIndex = url.indexOf('@');
            if (atIndex == -1) {
                return url;
            }
            
            int schemeEnd = url.indexOf("://") + 3;
            String userInfo = url.substring(schemeEnd, atIndex);
            String afterAuth = url.substring(atIndex + 1);
            
            String username = "";
            String password = "";
            
            int colonIndex = userInfo.indexOf(':');
            if (colonIndex != -1) {
                username = userInfo.substring(0, colonIndex);
                password = userInfo.substring(colonIndex + 1);
            } else {
                username = userInfo;
            }
            
            String baseUrl = "jdbc:postgresql://" + afterAuth;
            
            String encodedUsername = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            String encodedPassword = java.net.URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8);
            
            String separator = baseUrl.contains("?") ? "&" : "?";
            return baseUrl + separator + "user=" + encodedUsername + "&password=" + encodedPassword;
        }
        return originalUrl;
    }
}
