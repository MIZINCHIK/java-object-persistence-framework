package io.github.mizinchik.persistence.exceptions;

public class EmptyObjectInstantiationFailedException extends RuntimeException {
    private static final String MESSAGE = "Failed to instantiate a class via an empty constructor";

    public EmptyObjectInstantiationFailedException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
