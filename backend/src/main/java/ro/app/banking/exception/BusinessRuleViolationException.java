package ro.app.banking.exception;

/**
 * Exception thrown when a business rule is violated
 * (e.g., account already closed, balance not zero, etc.)
 */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException() {
        super();
    }

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
