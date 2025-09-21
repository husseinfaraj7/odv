package it.odvsicilia.backend.exception;

/**
 * Exception thrown when email delivery fails.
 */
public class EmailDeliveryException extends RuntimeException {
    
    private final String errorCode;
    
    public EmailDeliveryException(String message) {
        super(message);
        this.errorCode = "EMAIL_DELIVERY_FAILED";
    }
    
    public EmailDeliveryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EMAIL_DELIVERY_FAILED";
    }
    
    public EmailDeliveryException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}