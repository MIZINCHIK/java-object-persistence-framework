package io.github.mizinchik.persistence.deserialization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public record ClassData<T>(Class<T> clazz, String json) {
    public static <T> ClassData<T> of(Class<T> clazz, File json) {
        try {
            return new ClassData<>(clazz, Files.readString(json.toPath()));
        } catch (IOException e) {
            return null;
        }
    }
}
