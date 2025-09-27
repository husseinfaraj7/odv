package it.odvsicilia.backend.exception;

import java.util.Map;

public class EmailDeliveryException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;
    
    public EmailDeliveryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }
    
    public EmailDeliveryException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = null;
    }
    
    public EmailDeliveryException(String message, String errorCode, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    public EmailDeliveryException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
}