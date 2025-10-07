package it.odvsicilia.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFoundException(OrderNotFoundException ex) {
        logger.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Order not found", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOrderException(InvalidOrderException ex) {
        logger.warn("Invalid order: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid order", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOrderStatusException(InvalidOrderStatusException ex) {
        logger.warn("Invalid order status: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid order status", "message", ex.getMessage()));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleEmailDeliveryException(EmailDeliveryException ex) {
        logger.error("Email delivery failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Email delivery failed", "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        logger.warn("Type mismatch for parameter '{}': expected {} but received '{}'", 
                   ex.getName(), ex.getRequiredType().getSimpleName(), ex.getValue());
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                                     ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid parameter type", "message", message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        logger.warn("Method '{}' not supported for this endpoint. Supported methods: {}", 
                   ex.getMethod(), ex.getSupportedHttpMethods());
        String message = String.format("HTTP method '%s' is not supported. Supported methods: %s", 
                                     ex.getMethod(), ex.getSupportedHttpMethods());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "Method not allowed", "message", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.warn("Validation failed for request body: {}", ex.getMessage());
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.add(String.format("Field '%s': %s", error.getField(), error.getDefaultMessage()))
        );
        ex.getBindingResult().getGlobalErrors().forEach(error -> 
            errors.add(String.format("Object '%s': %s", error.getObjectName(), error.getDefaultMessage()))
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation failed", "message", "Request validation failed", "details", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        logger.warn("Constraint violations: {}", ex.getMessage());
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> String.format("Field '%s': %s", 
                                               violation.getPropertyPath(), 
                                               violation.getMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Constraint violation", "message", "Validation constraints violated", "details", errors));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        logger.debug("Static resource not found: {} - {}", ex.getResourcePath(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Resource not found", "message", "The requested resource was not found"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        logger.debug("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Endpoint not found", "message", "The requested endpoint was not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Don't log static resource errors as unexpected errors
        if (ex.getMessage() != null && ex.getMessage().contains("No static resource")) {
            logger.debug("Static resource error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resource not found", "message", "The requested resource was not found"));
        }
        
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", "An unexpected error occurred"));
    }
}