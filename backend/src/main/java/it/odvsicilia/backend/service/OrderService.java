package it.odvsicilia.backend.service;

import it.odvsicilia.backend.dto.OrderDto;
import it.odvsicilia.backend.dto.OrderItemDto;
import it.odvsicilia.backend.exception.InvalidOrderException;
import it.odvsicilia.backend.exception.InvalidOrderStatusException;
import it.odvsicilia.backend.exception.OrderNotFoundException;
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
        // Validate order data
        if (orderDto == null) {
            throw new InvalidOrderException("Dati ordine non possono essere null", "ORDER_DATA_NULL");
        }
        if (orderDto.getCustomerName() == null || orderDto.getCustomerName().trim().isEmpty()) {
            throw new InvalidOrderException("Nome cliente è obbligatorio", "CUSTOMER_NAME_REQUIRED");
        }
        if (orderDto.getCustomerEmail() == null || orderDto.getCustomerEmail().trim().isEmpty()) {
            throw new InvalidOrderException("Email cliente è obbligatoria", "CUSTOMER_EMAIL_REQUIRED");
        }
        if (orderDto.getItems() == null || orderDto.getItems().isEmpty()) {
            throw new InvalidOrderException("Ordine deve contenere almeno un prodotto", "ORDER_ITEMS_REQUIRED");
        }
        if (orderDto.getTotalAmount() == null || orderDto.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Totale ordine deve essere maggiore di zero", "INVALID_ORDER_TOTAL");
        }
        
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
    
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
    
    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new InvalidOrderException("Numero ordine non può essere vuoto", "ORDER_NUMBER_REQUIRED");
        }
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new OrderNotFoundException("Ordine con numero " + orderNumber + " non trovato", "ORDER_NOT_FOUND"));
    }
    
    public boolean updateOrderStatus(String orderNumber, String status) {
        // Validate order number format
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new InvalidOrderException("Order number cannot be null or empty", "ORDER_NUMBER_REQUIRED");
        }
        
        String trimmedOrderNumber = orderNumber.trim();
        if (!trimmedOrderNumber.matches("^ODV\\d{14}$")) {
            throw new InvalidOrderException("Order number format is invalid: " + trimmedOrderNumber + 
                ". Expected format: ODVyyyyMMddHHmmss", "INVALID_ORDER_NUMBER_FORMAT");
        }
        
        // Validate status
        if (status == null || status.trim().isEmpty()) {
            throw new InvalidOrderStatusException("Order status cannot be null or empty", "ORDER_STATUS_REQUIRED");
        }
        
        String trimmedStatus = status.trim().toUpperCase();
        Order.OrderStatus newStatus;
        try {
            newStatus = Order.OrderStatus.valueOf(trimmedStatus);
        } catch (IllegalArgumentException e) {
            String validStatuses = java.util.Arrays.stream(Order.OrderStatus.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));
            throw new InvalidOrderStatusException("Invalid order status: " + status + 
                ". Valid statuses are: " + validStatuses, "INVALID_ORDER_STATUS", e);
        }
        
        // Check if order exists in database
        Order order = orderRepository.findByOrderNumber(trimmedOrderNumber)
            .orElseThrow(() -> new OrderNotFoundException("Order with number " + trimmedOrderNumber + 
                " was not found in the database", "ORDER_NOT_FOUND"));
        
        // Validate status transition
        Order.OrderStatus currentStatus = order.getStatus();
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new InvalidOrderStatusException("Invalid order status transition: " + 
                currentStatus + " cannot be changed to " + newStatus, "INVALID_STATUS_TRANSITION");
        }
        
        order.setStatus(newStatus);
        orderRepository.save(order);
        return true;
    }
    
    private boolean isValidStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        // Define valid status transitions
        switch (currentStatus) {
            case PENDING:
                return newStatus == Order.OrderStatus.CONFIRMED || 
                       newStatus == Order.OrderStatus.CANCELLED;
            case CONFIRMED:
                return newStatus == Order.OrderStatus.PROCESSING || 
                       newStatus == Order.OrderStatus.CANCELLED;
            case PROCESSING:
                return newStatus == Order.OrderStatus.SHIPPED || 
                       newStatus == Order.OrderStatus.CANCELLED;
            case SHIPPED:
                return newStatus == Order.OrderStatus.DELIVERED;
            case DELIVERED:
                // Delivered orders cannot change status
                return false;
            case CANCELLED:
                // Cancelled orders cannot change status
                return false;
            default:
                return false;
        }
    }
    
    
    @Transactional(readOnly = true)
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
