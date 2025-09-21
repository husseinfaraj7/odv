package it.odvsicilia.backend.exception;

/**
 * Exception thrown when an invalid order status is provided.
 */
public class InvalidOrderStatusException extends RuntimeException {
    
    private final String errorCode;
    
    public InvalidOrderStatusException(String message) {
        super(message);
        this.errorCode = "INVALID_ORDER_STATUS";
    }
    
    public InvalidOrderStatusException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public InvalidOrderStatusException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_ORDER_STATUS";
    }
    
    public InvalidOrderStatusException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}