package io.github.mizinchik.persistence.exceptions;

public class FailedCollectionDeserializationException extends IllegalStateException {
    private static final String MESSAGE = "Failed to deserialize a collection";

    public FailedCollectionDeserializationException(Throwable cause) {
        super(MESSAGE, cause);
    }

    public FailedCollectionDeserializationException() {
        super(MESSAGE);
    }
}
