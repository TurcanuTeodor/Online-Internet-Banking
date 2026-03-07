package ro.app.client.exception;

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
