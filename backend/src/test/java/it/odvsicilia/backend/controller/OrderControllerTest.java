package it.odvsicilia.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.odvsicilia.backend.config.GlobalExceptionHandler;
import it.odvsicilia.backend.exception.InvalidOrderException;
import it.odvsicilia.backend.exception.InvalidOrderStatusException;
import it.odvsicilia.backend.exception.OrderNotFoundException;
import it.odvsicilia.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        OrderController orderController = new OrderController();
        // Use reflection to set the private field
        org.springframework.test.util.ReflectionTestUtils.setField(orderController, "orderService", orderService);
        
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        objectMapper = new ObjectMapper();
    }

    @Test
    void updateOrderStatus_ShouldReturn404_WhenOrderNotFoundException() throws Exception {
        // Given
        String orderNumber = "ODV12345";
        String newStatus = "SHIPPED";
        Map<String, String> statusUpdate = Map.of("status", newStatus);
        
        when(orderService.updateOrderStatus(eq(orderNumber), eq(newStatus)))
                .thenThrow(new OrderNotFoundException("Ordine con numero " + orderNumber + " non trovato", "ORDER_NOT_FOUND"));
        
        // When & Then
        mockMvc.perform(put("/api/orders/{orderNumber}/status", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ordine non trovato"))
                .andExpect(jsonPath("$.error").value("Ordine con numero " + orderNumber + " non trovato"));
    }

    @Test
    void updateOrderStatus_ShouldReturn400_WhenInvalidOrderException() throws Exception {
        // Given
        String orderNumber = "ODV12345";
        String newStatus = "SHIPPED";
        Map<String, String> statusUpdate = Map.of("status", newStatus);
        
        when(orderService.updateOrderStatus(eq(orderNumber), eq(newStatus)))
                .thenThrow(new InvalidOrderException("Numero ordine non può essere vuoto", "ORDER_NUMBER_REQUIRED"));
        
        // When & Then
        mockMvc.perform(put("/api/orders/{orderNumber}/status", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dati dell'ordine non validi"))
                .andExpect(jsonPath("$.error").value("Numero ordine non può essere vuoto"));
    }

    @Test
    void updateOrderStatus_ShouldReturn400_WhenInvalidOrderStatusException() throws Exception {
        // Given
        String orderNumber = "ODV12345";
        String newStatus = "INVALID_STATUS";
        Map<String, String> statusUpdate = Map.of("status", newStatus);
        
        when(orderService.updateOrderStatus(eq(orderNumber), eq(newStatus)))
                .thenThrow(new InvalidOrderStatusException("Status ordine non valido: " + newStatus, "INVALID_ORDER_STATUS"));
        
        // When & Then
        mockMvc.perform(put("/api/orders/{orderNumber}/status", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Stato ordine non valido"))
                .andExpect(jsonPath("$.error").value("Status ordine non valido: " + newStatus));
    }
}