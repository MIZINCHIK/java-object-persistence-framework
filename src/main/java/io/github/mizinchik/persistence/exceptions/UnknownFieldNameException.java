package io.github.mizinchik.persistence.exceptions;

public class UnknownFieldNameException extends RuntimeException {
    private static final String MESSAGE = "Found a field with a name not present in JSON";

    public UnknownFieldNameException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
