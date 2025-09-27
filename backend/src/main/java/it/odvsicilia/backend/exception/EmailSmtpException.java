package it.odvsicilia.backend.exception;

import java.util.Map;

public class EmailSmtpException extends EmailDeliveryException {
    
    public EmailSmtpException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public EmailSmtpException(String message, String errorCode, Map<String, Object> context) {
        super(message, errorCode, context);
    }
    
    public EmailSmtpException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, context, cause);
    }
}