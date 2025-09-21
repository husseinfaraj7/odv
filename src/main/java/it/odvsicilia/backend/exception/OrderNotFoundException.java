package it.odvsicilia.backend.exception;

/**
 * Exception thrown when an order is not found in the system.
 */
public class OrderNotFoundException extends RuntimeException {
    
    private final String errorCode;
    
    public OrderNotFoundException(String message) {
        super(message);
        this.errorCode = "ORDER_NOT_FOUND";
    }
    
    public OrderNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ORDER_NOT_FOUND";
    }
    
    public OrderNotFoundException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}