package io.github.mizinchik.persistence.session;

import io.github.mizinchik.persistence.deserialization.Deserializer;
import io.github.mizinchik.persistence.deserialization.SerialStream;
import io.github.mizinchik.persistence.filtering.AttributeFilter;
import java.nio.file.Path;
import java.util.List;
import org.json.JSONObject;

public interface Session {
    void setDirectory(Path directory);

    <T> void insert(T object);

    <T> List<T> find(Class<T> clazz);

    <T> List<T> find(Class<T> clazz, AttributeFilter filter);

    <T> void delete(Class<T> clazz, AttributeFilter filter);

    void persist();

    <T> Deserializer<T> getDeserializer(Object jsonObject, Class<T> clazz);

    <T> Deserializer<T> getDeserializer(JSONObject jsonObject, Class<T> clazz);

    <T> SerialStream<T> getSerializer(Class<T> clazz);
}
