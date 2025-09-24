package it.odvsicilia.backend.config;

import it.odvsicilia.backend.dto.ApiResponse;
import it.odvsicilia.backend.exception.InvalidOrderException;
import it.odvsicilia.backend.exception.InvalidOrderStatusException;
import it.odvsicilia.backend.exception.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleOrderNotFoundException(OrderNotFoundException ex) {
        logger.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Ordine non trovato", ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidOrderException(InvalidOrderException ex) {
        logger.warn("Invalid order: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dati dell'ordine non validi", ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidOrderStatusException(InvalidOrderStatusException ex) {
        logger.warn("Invalid order status: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Stato ordine non valido", ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {
        logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Errore interno del server", "Si è verificato un errore imprevisto"));
    }
}