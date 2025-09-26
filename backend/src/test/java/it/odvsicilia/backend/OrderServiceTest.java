package it.odvsicilia.backend;

import it.odvsicilia.backend.exception.InvalidOrderException;
import it.odvsicilia.backend.exception.InvalidOrderStatusException;
import it.odvsicilia.backend.exception.OrderNotFoundException;
import it.odvsicilia.backend.model.Order;
import it.odvsicilia.backend.repository.OrderRepository;
import it.odvsicilia.backend.service.EmailService;
import it.odvsicilia.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private String validOrderNumber;

    @BeforeEach
    void setUp() {
        validOrderNumber = "ODV20241224120000";
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber(validOrderNumber);
        testOrder.setCustomerName("Test Customer");
        testOrder.setCustomerEmail("test@example.com");
        testOrder.setShippingAddress("Test Address");
        testOrder.setShippingCity("Test City");
        testOrder.setShippingPostalCode("12345");
        testOrder.setShippingCountry("Italy");
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void updateOrderStatus_ValidInput_ShouldUpdateStatus() {
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        boolean result = orderService.updateOrderStatus(validOrderNumber, "CONFIRMED");

        assertTrue(result);
        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_NullOrderNumber_ShouldThrowInvalidOrderException() {
        InvalidOrderException exception = assertThrows(InvalidOrderException.class,
                () -> orderService.updateOrderStatus(null, "CONFIRMED"));

        assertEquals("Order number cannot be null or empty", exception.getMessage());
        assertEquals("ORDER_NUMBER_REQUIRED", exception.getErrorCode());
        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_EmptyOrderNumber_ShouldThrowInvalidOrderException() {
        InvalidOrderException exception = assertThrows(InvalidOrderException.class,
                () -> orderService.updateOrderStatus("", "CONFIRMED"));

        assertEquals("Order number cannot be null or empty", exception.getMessage());
        assertEquals("ORDER_NUMBER_REQUIRED", exception.getErrorCode());
        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_InvalidOrderNumberFormat_ShouldThrowInvalidOrderException() {
        String invalidOrderNumber = "INVALID123";
        
        InvalidOrderException exception = assertThrows(InvalidOrderException.class,
                () -> orderService.updateOrderStatus(invalidOrderNumber, "CONFIRMED"));

        assertEquals("Order number format is invalid: " + invalidOrderNumber + 
                ". Expected format: ODVyyyyMMddHHmmss", exception.getMessage());
        assertEquals("INVALID_ORDER_NUMBER_FORMAT", exception.getErrorCode());
        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_OrderNotFound_ShouldThrowOrderNotFoundException() {
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(validOrderNumber, "CONFIRMED"));

        assertEquals("Order with number " + validOrderNumber + " was not found in the database", 
                exception.getMessage());
        assertEquals("ORDER_NOT_FOUND", exception.getErrorCode());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_NullStatus_ShouldThrowInvalidOrderStatusException() {
        InvalidOrderStatusException exception = assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(validOrderNumber, null));

        assertEquals("Order status cannot be null or empty", exception.getMessage());
        assertEquals("ORDER_STATUS_REQUIRED", exception.getErrorCode());
        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_EmptyStatus_ShouldThrowInvalidOrderStatusException() {
        InvalidOrderStatusException exception = assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(validOrderNumber, ""));

        assertEquals("Order status cannot be null or empty", exception.getMessage());
        assertEquals("ORDER_STATUS_REQUIRED", exception.getErrorCode());
        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_InvalidStatus_ShouldThrowInvalidOrderStatusException() {
        String invalidStatus = "INVALID_STATUS";
        
        InvalidOrderStatusException exception = assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(validOrderNumber, invalidStatus));

        assertTrue(exception.getMessage().contains("Invalid order status: " + invalidStatus));
        assertTrue(exception.getMessage().contains("Valid statuses are:"));
        assertEquals("INVALID_ORDER_STATUS", exception.getErrorCode());
        verify(orderRepository, never()).findByOrderNumber(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_InvalidStatusTransition_CancelledToConfirmed_ShouldThrowException() {
        testOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.of(testOrder));

        InvalidOrderStatusException exception = assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(validOrderNumber, "CONFIRMED"));

        assertEquals("Invalid order status transition: CANCELLED cannot be changed to CONFIRMED", 
                exception.getMessage());
        assertEquals("INVALID_STATUS_TRANSITION", exception.getErrorCode());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_InvalidStatusTransition_DeliveredToShipped_ShouldThrowException() {
        testOrder.setStatus(Order.OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.of(testOrder));

        InvalidOrderStatusException exception = assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(validOrderNumber, "SHIPPED"));

        assertEquals("Invalid order status transition: DELIVERED cannot be changed to SHIPPED", 
                exception.getMessage());
        assertEquals("INVALID_STATUS_TRANSITION", exception.getErrorCode());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_ValidStatusTransition_PendingToConfirmed_ShouldSucceed() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        boolean result = orderService.updateOrderStatus(validOrderNumber, "CONFIRMED");

        assertTrue(result);
        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_ValidStatusTransition_ProcessingToShipped_ShouldSucceed() {
        testOrder.setStatus(Order.OrderStatus.PROCESSING);
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        boolean result = orderService.updateOrderStatus(validOrderNumber, "SHIPPED");

        assertTrue(result);
        assertEquals(Order.OrderStatus.SHIPPED, testOrder.getStatus());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_CaseInsensitiveStatus_ShouldWork() {
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        boolean result = orderService.updateOrderStatus(validOrderNumber, "confirmed");

        assertTrue(result);
        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_TrimWhitespace_ShouldWork() {
        when(orderRepository.findByOrderNumber(validOrderNumber)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        boolean result = orderService.updateOrderStatus("  " + validOrderNumber + "  ", "  CONFIRMED  ");

        assertTrue(result);
        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository).findByOrderNumber(validOrderNumber);
        verify(orderRepository).save(testOrder);
    }
}