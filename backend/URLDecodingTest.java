import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

/**
 * Standalone test to verify URL decoding functionality for database connection details
 */
public class URLDecodingTest {
    
    public static void main(String[] args) {
        System.out.println("Testing URL decoding functionality...\n");
        
        // Test case 1: URL with @ symbol (%40) and + symbol (%2B)
        testUrlDecoding("postgresql://user%40domain:pass%2Bword@localhost:5432/testdb", 
                       "user@domain", "pass+word");
        
        // Test case 2: URL with various special characters
        testUrlDecoding("postgresql://user%21%40%23:pass%24%25%5E%26%2A@localhost:5432/testdb", 
                       "user!@#", "pass$%^&*");
        
        // Test case 3: URL without encoding (should still work)
        testUrlDecoding("postgresql://normaluser:normalpass@localhost:5432/testdb", 
                       "normaluser", "normalpass");
        
        // Test case 4: Complex Supabase-like password
        testUrlDecoding("postgresql://postgres:aBc%40123%2BD%23fG%26h@db.supabasehost.co:5432/postgres", 
                       "postgres", "aBc@123+D#fG&h");
        
        System.out.println("All tests completed!");
    }
    
    private static void testUrlDecoding(String databaseUrl, String expectedUsername, String expectedPassword) {
        System.out.println("Testing URL: " + databaseUrl.replaceAll("://[^:]+:[^@]+@", "://***:***@"));
        
        try {
            ConnectionDetails details = parseConnectionDetails(databaseUrl);
            
            System.out.println("Expected username: " + expectedUsername);
            System.out.println("Actual username:   " + details.username);
            System.out.println("Username match: " + expectedUsername.equals(details.username));
            
            System.out.println("Expected password: " + expectedPassword);
            System.out.println("Actual password:   " + details.password);
            System.out.println("Password match: " + expectedPassword.equals(details.password));
            
            if (expectedUsername.equals(details.username) && expectedPassword.equals(details.password)) {
                System.out.println("✅ TEST PASSED\n");
            } else {
                System.out.println("❌ TEST FAILED\n");
            }
            
        } catch (Exception e) {
            System.out.println("❌ TEST FAILED with exception: " + e.getMessage() + "\n");
        }
    }
    
    private static ConnectionDetails parseConnectionDetails(String databaseUrl) {
        try {
            URI uri = new URI(databaseUrl);
            
            if (uri.getUserInfo() == null) {
                throw new IllegalArgumentException("No user information found in DATABASE_URL");
            }
            
            String[] userInfo = uri.getUserInfo().split(":");
            
            if (userInfo.length != 2) {
                throw new IllegalArgumentException("Invalid user information format in DATABASE_URL. Expected format: username:password");
            }
            
            String username = userInfo[0];
            String password = userInfo[1];
            
            // URL decode the username and password to handle encoded special characters
            try {
                username = URLDecoder.decode(username, "UTF-8");
                password = URLDecoder.decode(password, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // This should never happen with UTF-8, but handle gracefully
                System.out.println("Warning: Failed to URL decode username/password, using raw values: " + e.getMessage());
            }
            
            if (username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username is empty in DATABASE_URL");
            }
            
            if (password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password is empty in DATABASE_URL");
            }
            
            return new ConnectionDetails(username, password);
            
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to parse connection details from DATABASE_URL: " + e.getMessage(), e);
        }
    }
    
    private static class ConnectionDetails {
        final String username;
        final String password;
        
        ConnectionDetails(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}