package io.github.mizinchik.persistence.exceptions;

public class FailedMapDeserializationException extends IllegalStateException {
    private static final String MESSAGE = "Failed to deserialize a collection";

    public FailedMapDeserializationException(Throwable cause) {
        super(MESSAGE, cause);
    }

    public FailedMapDeserializationException() {
        super(MESSAGE);
    }
}
