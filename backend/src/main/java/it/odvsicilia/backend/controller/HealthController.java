package it.odvsicilia.backend.controller;

import it.odvsicilia.backend.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        Map<String, Object> status = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            status.put("database", "connected");
            status.put("url", connection.getMetaData().getURL());
            status.put("driver", connection.getMetaData().getDriverName());
            
            // Test a simple query
            long count = contactMessageRepository.count();
            status.put("query_test", "success");
            status.put("contact_messages_count", count);
            
            return ResponseEntity.ok(status);
        } catch (SQLException e) {
            status.put("database", "error");
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
}