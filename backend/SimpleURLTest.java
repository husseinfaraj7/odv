import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

public class SimpleURLTest {
    public static void main(String[] args) {
        // Test URL decoding
        testDecode("user%40domain", "user@domain");
        testDecode("pass%2Bword", "pass+word");
        testDecode("user%21%40%23", "user!@#");
        testDecode("pass%24%25%5E%26%2A", "pass$%^&*");
        testDecode("normaluser", "normaluser");
        testDecode("normalpass", "normalpass");
    }
    
    private static void testDecode(String encoded, String expected) {
        try {
            String decoded = URLDecoder.decode(encoded, "UTF-8");
            boolean matches = expected.equals(decoded);
            System.out.println("Input: " + encoded + " -> Output: " + decoded + 
                             " -> Expected: " + expected + " -> " + 
                             (matches ? "PASS" : "FAIL"));
        } catch (UnsupportedEncodingException e) {
            System.out.println("ENCODING ERROR for: " + encoded + " - " + e.getMessage());
        }
    }
}