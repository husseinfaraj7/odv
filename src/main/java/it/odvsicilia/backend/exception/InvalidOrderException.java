package it.odvsicilia.backend.exception;

/**
 * Exception thrown when an order contains invalid data or fails validation.
 */
public class InvalidOrderException extends RuntimeException {
    
    private final String errorCode;
    
    public InvalidOrderException(String message) {
        super(message);
        this.errorCode = "INVALID_ORDER_DATA";
    }
    
    public InvalidOrderException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_ORDER_DATA";
    }
    
    public InvalidOrderException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}