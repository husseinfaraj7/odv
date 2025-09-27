package it.odvsicilia.backend.exception;

import java.util.Map;

public class EmailInvalidRecipientException extends EmailDeliveryException {
    
    public EmailInvalidRecipientException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public EmailInvalidRecipientException(String message, String errorCode, Map<String, Object> context) {
        super(message, errorCode, context);
    }
    
    public EmailInvalidRecipientException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, context, cause);
    }
}