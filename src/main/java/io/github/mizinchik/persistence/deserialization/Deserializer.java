package io.github.mizinchik.persistence.deserialization;

import java.io.File;

public interface Deserializer {
    <T> SerialStream<T> deserialize(Class<T> clazz, String json);

    <T> SerialStream<T> deserialize(Class<T> clazz, File json);
}
