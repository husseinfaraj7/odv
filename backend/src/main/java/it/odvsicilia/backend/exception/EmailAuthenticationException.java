package it.odvsicilia.backend.exception;

import java.util.Map;

public class EmailAuthenticationException extends EmailDeliveryException {
    
    public EmailAuthenticationException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public EmailAuthenticationException(String message, String errorCode, Map<String, Object> context) {
        super(message, errorCode, context);
    }
    
    public EmailAuthenticationException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, context, cause);
    }
}