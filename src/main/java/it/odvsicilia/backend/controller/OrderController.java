package it.odvsicilia.backend.controller;

import it.odvsicilia.backend.dto.OrderDto;
import it.odvsicilia.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"https://odvsicilia.it", "http://localhost:3000"})
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderDto orderDto) {
        try {
            String orderNumber = orderService.createOrder(orderDto);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ordine creato con successo!",
                "orderNumber", orderNumber
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nella creazione dell'ordine: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            return ResponseEntity.ok(orderService.getAllOrders());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nel recupero degli ordini."
            ));
        }
    }
    
    @GetMapping("/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Ordine non trovato."
            ));
        }
    }
    
    @PutMapping("/{orderNumber}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestBody Map<String, String> statusUpdate) {
        try {
            orderService.updateOrderStatus(orderNumber, statusUpdate.get("status"));
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Stato ordine aggiornato con successo."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nell'aggiornamento dello stato."
            ));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getOrderStats() {
        try {
            return ResponseEntity.ok(orderService.getOrderStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Errore nel recupero delle statistiche."
            ));
        }
    }
}
