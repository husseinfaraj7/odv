package it.odvsicilia.backend.exception;

/**
 * Exception thrown when a contact message is not found in the system.
 */
public class ContactNotFoundException extends RuntimeException {
    
    private final String errorCode;
    
    public ContactNotFoundException(String message) {
        super(message);
        this.errorCode = "CONTACT_NOT_FOUND";
    }
    
    public ContactNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ContactNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CONTACT_NOT_FOUND";
    }
    
    public ContactNotFoundException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}