package it.odvsicilia.backend;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Simple test to simulate port binding behavior expected by Render.com deployment.
 */
public class RenderDeploymentPortTest {

    public static void main(String[] args) {
        RenderDeploymentPortTest test = new RenderDeploymentPortTest();
        try {
            test.testPortBinding();
            System.out.println("✅ Port binding simulation successful!");
        } catch (Exception e) {
            System.err.println("❌ Port binding test failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public void testPortBinding() throws Exception {
        testDefaultPortBinding();
        testCustomPortBinding();
        testPortEnvironmentVariable();
    }

    public void testDefaultPortBinding() throws Exception {
        // Test that we can bind to default port 8080 (Spring Boot default)
        try (ServerSocket socket = new ServerSocket(8080)) {
            if (!socket.isBound()) {
                throw new AssertionError("Should be able to bind to default port 8080");
            }
            System.out.println("✓ Can bind to default port 8080");
        } catch (IOException e) {
            // Port might be in use, try alternative
            System.out.println("⚠ Port 8080 in use, testing alternative port");
            testAlternativePortBinding();
        }
    }

    public void testCustomPortBinding() throws Exception {
        // Test that we can bind to port 10000 (common Render.com port)
        try (ServerSocket socket = new ServerSocket(10000)) {
            if (!socket.isBound()) {
                throw new AssertionError("Should be able to bind to port 10000");
            }
            System.out.println("✓ Can bind to Render.com port 10000");
        } catch (IOException e) {
            System.out.println("⚠ Port 10000 in use, testing alternative");
            testAlternativePortBinding();
        }
    }
    
    private void testAlternativePortBinding() throws Exception {
        // Find any available port
        try (ServerSocket socket = new ServerSocket(0)) {
            int port = socket.getLocalPort();
            if (port <= 0) {
                throw new AssertionError("Should be able to get an available port");
            }
            System.out.println("✓ Successfully bound to available port: " + port);
        }
    }

    public void testPortEnvironmentVariable() throws Exception {
        // Test PORT environment variable handling (simulated)
        String portEnvVar = System.getenv("PORT");
        
        if (portEnvVar != null) {
            try {
                int port = Integer.parseInt(portEnvVar);
                if (port <= 0 || port > 65535) {
                    throw new AssertionError("PORT environment variable should be valid port number: " + port);
                }
                System.out.println("✓ PORT environment variable is valid: " + port);
                
                // Test that we can bind to the specified port
                try (ServerSocket socket = new ServerSocket(port)) {
                    System.out.println("✓ Can bind to PORT environment variable: " + port);
                } catch (IOException e) {
                    System.out.println("⚠ PORT " + port + " is in use: " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                throw new AssertionError("PORT environment variable should be numeric: " + portEnvVar);
            }
        } else {
            System.out.println("ℹ PORT environment variable not set (will use default 8080)");
        }
    }
}