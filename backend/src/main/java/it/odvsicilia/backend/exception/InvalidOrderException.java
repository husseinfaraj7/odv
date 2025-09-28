package it.odvsicilia.backend.exception;

public class InvalidOrderException extends RuntimeException {
    private final String errorCode;
    
    public InvalidOrderException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public InvalidOrderException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}