package io.github.mizinchik.persistence.deserialization;

import io.github.mizinchik.persistence.filtering.AttributeFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.json.JSONObject;

public class JsonSerialStream<T> implements SerialStream<T> {
    private final List<JsonDeserializer<T>> objects;
    private final List<File> files;
    private final Class<T> clazz;

    public JsonSerialStream(Class<T> clazz) {
        this.clazz = clazz;
        objects = new ArrayList<>();
        files = new ArrayList<>();
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
        files.add(json);
        return this;
    }

    @Override
    public SerialStream<T> addAllFiles(Collection<File> jsons) {
        files.addAll(jsons);
        return this;
    }

    @Override
    public List<T> toList() {
        return obtainDeserialStream().map(JsonDeserializer::instance)
                .toList();
    }

    @Override
    public List<T> toList(AttributeFilter filter) {
        return obtainDeserialStream()
                .filter(object ->
                        filter.evaluate(object.getRelevantFields(filter.getNecessaryFields())))
                .map(JsonDeserializer::instance)
                .toList();
    }

    @Override
    public List<T> toListExclude(AttributeFilter filter) {
        return obtainDeserialStream()
                .filter(object ->
                        !filter.evaluate(object.getRelevantFields(filter.getNecessaryFields())))
                .map(JsonDeserializer::instance)
                .toList();
    }

    private Stream<JsonDeserializer<T>> obtainDeserialStream() {
        return Stream.concat(
                objects.stream(),
                files.stream()
                        .map(file -> {
                            try {
                                return new JsonDeserializer<>(
                                        new JSONObject(Files.readString(file.toPath())),
                                        clazz);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
        );
    }
}
