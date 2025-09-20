package it.odvsicilia.backend.service;

import it.odvsicilia.backend.dto.OrderDto;
import it.odvsicilia.backend.dto.OrderItemDto;
import it.odvsicilia.backend.model.Order;
import it.odvsicilia.backend.model.OrderItem;
import it.odvsicilia.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private EmailService emailService;
    
    public String createOrder(OrderDto orderDto) {
        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerName(orderDto.getCustomerName());
        order.setCustomerEmail(orderDto.getCustomerEmail());
        order.setCustomerPhone(orderDto.getCustomerPhone());
        order.setShippingAddress(orderDto.getShippingAddress());
        order.setShippingCity(orderDto.getShippingCity());
        order.setShippingPostalCode(orderDto.getShippingPostalCode());
        order.setShippingCountry(orderDto.getShippingCountry());
        order.setTotalAmount(orderDto.getTotalAmount());
        order.setPaymentMethod(orderDto.getPaymentMethod());
        order.setNotes(orderDto.getNotes());
        
        // Create order items
        List<OrderItem> items = orderDto.getItems().stream()
            .map(itemDto -> {
                OrderItem item = new OrderItem(
                    itemDto.getProductName(),
                    itemDto.getQuantity(),
                    itemDto.getUnitPrice()
                );
                item.setProductDescription(itemDto.getProductDescription());
                item.setProductSku(itemDto.getProductSku());
                item.setOrder(order);
                return item;
            })
            .collect(Collectors.toList());
        
        order.setItems(items);
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Send emails
        try {
            String itemsText = formatOrderItemsForEmail(items);
            
            // Send notification to admin
            emailService.sendOrderNotificationToAdmin(
                savedOrder.getOrderNumber(),
                savedOrder.getCustomerName(),
                savedOrder.getCustomerEmail(),
                savedOrder.getTotalAmount().toString(),
                itemsText
            );
            
            // Send confirmation to customer
            emailService.sendOrderConfirmationToCustomer(
                savedOrder.getCustomerName(),
                savedOrder.getCustomerEmail(),
                savedOrder.getOrderNumber(),
                savedOrder.getTotalAmount().toString(),
                itemsText
            );
        } catch (Exception e) {
            System.err.println("Errore nell'invio delle email ordine: " + e.getMessage());
            // Don't fail the entire operation if email fails
        }
        
        return savedOrder.getOrderNumber();
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Ordine non trovato"));
    }
    
    public Order updateOrderStatus(String orderNumber, String status) {
        Order order = getOrderByNumber(orderNumber);
        order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        return orderRepository.save(order);
    }
    
    public Map<String, Object> getOrderStats() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countPendingOrders();
        long pendingPayments = orderRepository.countPendingPayments();
        
        return Map.of(
            "totalOrders", totalOrders,
            "pendingOrders", pendingOrders,
            "pendingPayments", pendingPayments
        );
    }
    
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ODV" + timestamp;
    }
    
    private String formatOrderItemsForEmail(List<OrderItem> items) {
        return items.stream()
            .map(item -> String.format("• %s - Quantità: %d - Prezzo: €%.2f - Totale: €%.2f",
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()))
            .collect(Collectors.joining("\n"));
    }
}
