package io.github.mizinchik.persistence.filtering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class AttributeFilter {
    private final String attributeName;
    private final List<AttributeFilter> filters;
    private final Set<String> necessaryFields;
    private Predicate predicate;
    private boolean isAnd;

    public AttributeFilter(String attributeName, Predicate<?> predicate) {
        this.attributeName = attributeName;
        this.predicate = predicate;
        filters = new ArrayList<>();
        necessaryFields = new HashSet<>();
        necessaryFields.add(attributeName);
        isAnd = true;
    }

    public Set<String> getNecessaryFields() {
        return necessaryFields;
    }

    public AttributeFilter not() {
        predicate = predicate.negate();
        for (var sub : filters) {
            sub.not();
            sub.reverseAnd();
        }
        return this;
    }

    public void setOr() {
        isAnd = false;
    }

    public void setAnd() {
        isAnd = true;
    }

    public void reverseAnd() {
        isAnd = !isAnd;
    }

    public AttributeFilter and(AttributeFilter filter) {
        filter.setAnd();
        filters.add(filter);
        necessaryFields.addAll(filter.getNecessaryFields());
        return this;
    }

    public AttributeFilter or(AttributeFilter filter) {
        filter.setOr();
        filters.add(filter);
        necessaryFields.addAll(filter.getNecessaryFields());
        return this;
    }

    public boolean evaluate(Map<String, Object> necessaryObjects) {
        try {
            if (!necessaryObjects.keySet().containsAll(necessaryFields)) {
                throw new RuntimeException();
            }
            boolean result = predicate.test(necessaryObjects.get(attributeName));
            for (var filter : filters) {
                boolean temporary = filter.evaluate(necessaryObjects);
                if (filter.isAnd) {
                    result = result && temporary;
                } else {
                    result = result || temporary;
                }
            }
            return result;
        } catch (Exception ignored) {
            return false;
        }
    }
}
