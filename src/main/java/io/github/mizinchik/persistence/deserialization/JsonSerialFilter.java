package io.github.mizinchik.persistence.deserialization;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class JsonSerialFilter implements SerialFilter {
    private final Map<String, Predicate> attributeFilters;

    public JsonSerialFilter(String attributeName, Predicate<?> predicate) {
        attributeFilters = new HashMap<>();
        attributeFilters.put(attributeName, predicate);
    }

    @Override
    public JsonSerialFilter and(String attributeName, Predicate<?> predicate) {
        attributeFilters.put(attributeName, attributeFilters.getOrDefault(attributeName, (obj) -> true).and(predicate));
        return this;
    }

    @Override
    public JsonSerialFilter or(String attributeName, Predicate<?> predicate) {
        attributeFilters.put(attributeName, attributeFilters.getOrDefault(attributeName, (obj) -> true).or(predicate));
        return this;
    }

    @Override
    public JsonSerialFilter negate(String attributeName) {
        attributeFilters.put(attributeName, attributeFilters.getOrDefault(attributeName, (obj) -> true).negate());
        return this;
    }

    @Override
    public Map<String, Predicate> getFilters() {
        return attributeFilters;
    }
}
