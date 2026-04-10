package ro.app.account.exception;

public class StepUpRequiredException extends RuntimeException {
    public StepUpRequiredException(String message) {
        super(message);
    }
}
