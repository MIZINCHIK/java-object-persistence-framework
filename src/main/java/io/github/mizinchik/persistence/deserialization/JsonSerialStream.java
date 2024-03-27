package io.github.mizinchik.persistence.deserialization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.json.JSONObject;

public class JsonSerialStream<T> implements SerialStream<T> {
    private final List<JsonDeserializer<T>> objects;

    public JsonSerialStream() {
        objects = new ArrayList<>();
    }

    @Override
    public SerialStream<T> add(ClassData<T> json) {
        objects.add(new JsonDeserializer<>(new JSONObject(json.json()), json.clazz()));
        return this;
    }

    @Override
    public SerialStream<T> addAll(Collection<ClassData<T>> jsons) {
        objects.addAll(jsons
                .stream()
                .map(json -> new JsonDeserializer<>(
                        new JSONObject(
                                json.json()), json.clazz()
                ))
                .toList());
        return this;
    }

    @Override
    public List<T> toList() {
        return objects
                .stream()
                .map(JsonDeserializer::instance)
                .toList();
    }

    @Override
    public List<T> toList(SerialFilter<T> filter) {
        Map<String, Predicate> filters = filter.getFilters();
        return objects
                .stream()
                .filter(object -> filters
                        .keySet()
                        .stream()
                        .allMatch(key -> object.isValid(key, filters.get(key))))
                .map(JsonDeserializer::instance)
                .toList();
    }
}
