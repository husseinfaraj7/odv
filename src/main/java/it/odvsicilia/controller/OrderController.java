package it.odvsicilia.controller;

import it.odvsicilia.model.Order;
import it.odvsicilia.repository.OrderRepository;
import it.odvsicilia.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "${cors.allowed.origins}")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody Order order) {
        try {
            Order savedOrder = orderRepository.save(order);
            
            // Invia email di conferma
            emailService.sendOrderConfirmation(
                order.getCustomerEmail(), 
                order.getCustomerName(), 
                order.getTotalAmount().toString()
            );
            
            // Notifica admin
            String adminContent = String.format("""
                <h3>Nuovo Ordine Ricevuto</h3>
                <p><strong>Cliente:</strong> %s</p>
                <p><strong>Email:</strong> %s</p>
                <p><strong>Telefono:</strong> %s</p>
                <p><strong>Indirizzo:</strong> %s</p>
                <p><strong>Totale:</strong> €%s</p>
                <p><strong>Prodotti:</strong> %s</p>
                """, 
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getCustomerAddress(),
                order.getTotalAmount(),
                order.getProducts()
            );
            
            emailService.sendAdminNotification("Nuovo Ordine - ODV Sicilia", adminContent);
            
            return ResponseEntity.ok().body("{\"success\": \"Ordine ricevuto! Ti contatteremo presto.\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"Errore nell'invio dell'ordine: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody String status) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    orderRepository.save(order);
                    return ResponseEntity.ok().body("{\"success\": \"Stato aggiornato\"}");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}