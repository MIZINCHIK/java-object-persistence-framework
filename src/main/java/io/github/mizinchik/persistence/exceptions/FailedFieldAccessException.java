package io.github.mizinchik.persistence.exceptions;

public class FailedFieldAccessException extends RuntimeException {
    private static final String MESSAGE = "Failed to set a field";

    public FailedFieldAccessException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
