package ro.app.banking.exception;

/**
 * Exception thrown when a user tries to access resources they don't own
 */
public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException() {
        super("Access forbidden: you don't have permission to access this resource");
    }

    public ForbiddenAccessException(String message) {
        super(message);
    }

    public ForbiddenAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
