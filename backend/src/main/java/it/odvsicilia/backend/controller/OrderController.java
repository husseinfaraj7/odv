package it.odvsicilia.backend.controller;

import it.odvsicilia.backend.dto.ApiResponse;
import it.odvsicilia.backend.dto.OrderDto;
import it.odvsicilia.backend.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"https://odvsicilia.it", "http://localhost:3000"})
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, String>>> createOrder(@Valid @RequestBody OrderDto orderDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Errori di validazione");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dati dell'ordine non validi", errorMessage));
        }
        
        try {
            String orderNumber = orderService.createOrder(orderDto);
            Map<String, String> responseData = Map.of("orderNumber", orderNumber);
            return ResponseEntity.ok(
                ApiResponse.success("Ordine creato con successo!", responseData)
            );
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error in order creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dati dell'ordine non validi", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nella creazione dell'ordine. Riprova più tardi."));
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllOrders() {
        try {
            return ResponseEntity.ok(
                ApiResponse.success("Ordini recuperati con successo", orderService.getAllOrders())
            );
        } catch (Exception e) {
            logger.error("Error retrieving all orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nel recupero degli ordini."));
        }
    }
    
    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<?>> getOrderByNumber(@PathVariable String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Numero ordine non valido", "Il numero dell'ordine non può essere vuoto"));
        }
        
        try {
            Object order = orderService.getOrderByNumber(orderNumber);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Ordine non trovato", "Nessun ordine trovato con il numero specificato"));
            }
            return ResponseEntity.ok(
                ApiResponse.success("Ordine recuperato con successo", order)
            );
        } catch (Exception e) {
            logger.error("Error retrieving order by number: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nel recupero dell'ordine."));
        }
    }
    
    @PutMapping("/{orderNumber}/status")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestBody Map<String, String> statusUpdate) {
        
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Numero ordine non valido", "Il numero dell'ordine non può essere vuoto"));
        }
        
        String status = statusUpdate.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Stato non valido", "Lo stato dell'ordine non può essere vuoto"));
        }
        
        try {
            boolean updated = orderService.updateOrderStatus(orderNumber, status);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Ordine non trovato", "Nessun ordine trovato con il numero specificato"));
            }
            return ResponseEntity.ok(
                ApiResponse.success("Stato ordine aggiornato con successo.")
            );
        } catch (Exception e) {
            logger.error("Error updating order status for order: {}", orderNumber, e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getOrderStats() {
        try {
            return ResponseEntity.ok(
                ApiResponse.success("Statistiche recuperate con successo", orderService.getOrderStats())
            );
        } catch (Exception e) {
            logger.error("Error retrieving order stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Errore nel recupero delle statistiche."));
        }
    }
}
