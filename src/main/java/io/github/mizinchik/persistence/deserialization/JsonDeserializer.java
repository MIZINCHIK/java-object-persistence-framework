package io.github.mizinchik.persistence.deserialization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.json.JSONObject;

public class JsonDeserializer {
    public <T> JsonSerialStream<T> deserialize(Class<T> clazz, String json) {
        return new JsonSerialStream<>(new JSONObject(json), clazz);
    }

    public <T> JsonSerialStream<T> deserialize(Class<T> clazz, File json) throws IOException {
        return new JsonSerialStream<>(new JSONObject(Files.readString(json.toPath())), clazz);
    }
}
