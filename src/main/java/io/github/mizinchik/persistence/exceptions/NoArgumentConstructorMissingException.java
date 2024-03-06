package io.github.mizinchik.persistence.exceptions;

public class NoArgumentConstructorMissingException extends RuntimeException {
    private static final String MESSAGE =
            "The are to be deserialized objects must have a no argument constructor";

    public NoArgumentConstructorMissingException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
