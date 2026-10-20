package dev.as_request.exception;

public class TechnicianServiceException extends RuntimeException {

    public TechnicianServiceException(String message) {
        super(message);
    }

    public TechnicianServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
