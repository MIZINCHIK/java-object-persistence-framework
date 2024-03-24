package io.github.mizinchik.persistence.exceptions;

public class UnsupportedParametrizedMap extends IllegalStateException {
    private static final String MESSAGE =
            "Persistence framework only supports maps with two generic type parameters";

    public UnsupportedParametrizedMap() {
        super(MESSAGE);
    }
}
