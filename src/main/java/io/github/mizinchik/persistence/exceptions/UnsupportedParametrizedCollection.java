package io.github.mizinchik.persistence.exceptions;

public class UnsupportedParametrizedCollection extends IllegalStateException {
    private static final String MESSAGE =
            "Persistence framework only supports collections with a single generic type parameter";

    public UnsupportedParametrizedCollection() {
        super(MESSAGE);
    }
}
