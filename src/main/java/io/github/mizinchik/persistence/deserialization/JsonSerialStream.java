package io.github.mizinchik.persistence.deserialization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.json.JSONObject;

public class JsonSerialStream<T> implements SerialStream<T> {
    private final List<JsonDeserializer<T>> objects;
    private final Class<T> clazz;

    public JsonSerialStream(Class<T> clazz) {
        this.clazz = clazz;
        objects = new ArrayList<>();
    }

    @Override
    public SerialStream<T> add(String json) {
        objects.add(new JsonDeserializer<>(new JSONObject(json), clazz));
        return this;
    }

    @Override
    public SerialStream<T> addAll(Collection<String> jsons) {
        objects.addAll(jsons
                .stream()
                .map(json -> new JsonDeserializer<>(
                        new JSONObject(
                                json), clazz
                ))
                .toList());
        return this;
    }

    @Override
    public SerialStream<T> add(File json) {
        try {
            return add(Files.readString(json.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SerialStream<T> addAllFiles(Collection<File> jsons) {
        return addAll(jsons
                .stream()
                .map(file -> {
                    try {
                        return Files.readString(file.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList());
    }

    @Override
    public List<T> toList() {
        return objects
                .stream()
                .map(JsonDeserializer::instance)
                .toList();
    }

    @Override
    public List<T> toList(SerialFilter filter) {
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
