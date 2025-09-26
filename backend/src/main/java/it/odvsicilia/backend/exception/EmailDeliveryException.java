package it.odvsicilia.backend.exception;

public class EmailDeliveryException extends RuntimeException {
    private final String errorCode;
    
    public EmailDeliveryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public EmailDeliveryException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}