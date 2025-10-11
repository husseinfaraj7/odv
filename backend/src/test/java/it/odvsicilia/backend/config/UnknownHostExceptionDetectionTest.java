package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UnknownHostExceptionDetectionTest {

    @Test
    void testUnknownHostExceptionDetection() {
        SQLException sqlException = new SQLException("Connection failed");
        sqlException.initCause(new UnknownHostException("invalid-hostname.supabase.com"));
        
        assertTrue(containsUnknownHostException(sqlException), 
            "Should detect UnknownHostException in exception chain");
    }
    
    @Test
    void testConnectExceptionNotDetectedAsUnknownHost() {
        SQLException sqlException = new SQLException("Connection failed");
        sqlException.initCause(new ConnectException("Connection refused"));
        
        assertFalse(containsUnknownHostException(sqlException), 
            "Should not detect ConnectException as UnknownHostException");
    }
    
    @Test
    void testSocketTimeoutExceptionNotDetectedAsUnknownHost() {
        SQLException sqlException = new SQLException("Connection failed");
        sqlException.initCause(new SocketTimeoutException("Connection timeout"));
        
        assertFalse(containsUnknownHostException(sqlException), 
            "Should not detect SocketTimeoutException as UnknownHostException");
    }
    
    @Test
    void testHostnameExtractionFromJdbcUrl() {
        String poolerUrl = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres";
        String directUrl = "jdbc:postgresql://db.aws-0-us-east-1.supabase.co:5432/postgres";
        String invalidUrl = "jdbc:postgresql://malformed-host:5432/postgres";
        
        assertEquals("aws-0-us-east-1.pooler.supabase.com", extractHostnameFromUrl(poolerUrl));
        assertEquals("db.aws-0-us-east-1.supabase.co", extractHostnameFromUrl(directUrl));
        assertEquals("malformed-host", extractHostnameFromUrl(invalidUrl));
        assertEquals("unknown", extractHostnameFromUrl(null));
    }
    
    @Test
    void testRootCauseExtraction() {
        SQLException sqlException = new SQLException("SQL error");
        RuntimeException runtimeException = new RuntimeException("Runtime error", sqlException);
        UnknownHostException unknownHostException = new UnknownHostException("DNS failure");
        sqlException.initCause(unknownHostException);
        
        Throwable rootCause = getRootCause(runtimeException);
        assertTrue(rootCause instanceof UnknownHostException, 
            "Should extract UnknownHostException as root cause");
    }
    
    @Test
    void testRootCauseWithoutCause() {
        ConnectException connectException = new ConnectException("Connection refused");
        
        Throwable rootCause = getRootCause(connectException);
        assertEquals(connectException, rootCause, 
            "Should return the same exception if no cause");
    }
    
    private boolean containsUnknownHostException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
    
    private String extractHostnameFromUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "unknown";
        }
        Pattern hostPattern = Pattern.compile("jdbc:postgresql://([^:/]+)");
        Matcher matcher = hostPattern.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }
    
    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
