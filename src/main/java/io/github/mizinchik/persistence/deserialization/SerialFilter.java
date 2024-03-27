package io.github.mizinchik.persistence.deserialization;

import java.util.Map;
import java.util.function.Predicate;

public interface SerialFilter {
    SerialFilter and(String attributeName, Predicate<?> predicate);

    SerialFilter or(String attributeName, Predicate<?> predicate);

    SerialFilter negate(String attributeName);

    Map<String, Predicate> getFilters();
}
