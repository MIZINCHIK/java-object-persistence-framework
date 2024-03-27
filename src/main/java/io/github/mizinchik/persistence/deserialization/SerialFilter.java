package io.github.mizinchik.persistence.deserialization;

import java.util.Map;
import java.util.function.Predicate;

public interface SerialFilter<T> {
    SerialFilter<T> and(String attributeName, Predicate predicate);

    SerialFilter<T> or(String attributeName, Predicate predicate);

    SerialFilter<T> negate(String attributeName);

    Map<String, Predicate> getFilters();
}
