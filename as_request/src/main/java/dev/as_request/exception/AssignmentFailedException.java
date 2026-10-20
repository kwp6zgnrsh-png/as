package dev.as_request.exception;

public class AssignmentFailedException extends RuntimeException {

    public AssignmentFailedException(String message) {
        super(message);
    }

    public AssignmentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
