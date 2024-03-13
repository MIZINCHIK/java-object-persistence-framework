package io.github.mizinchik.persistence.exceptions;

public class ParametrizedTypeDeserializationException extends IllegalArgumentException {
    private static final String MESSAGE = "Can't obtain serial stream from a parametrized class";

    public ParametrizedTypeDeserializationException() {
        super(MESSAGE);
    }
}
