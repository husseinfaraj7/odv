package it.odvsicilia.backend.exception;

import java.util.Map;

public class EmailApiTimeoutException extends EmailDeliveryException {
    
    public EmailApiTimeoutException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public EmailApiTimeoutException(String message, String errorCode, Map<String, Object> context) {
        super(message, errorCode, context);
    }
    
    public EmailApiTimeoutException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, context, cause);
    }
}