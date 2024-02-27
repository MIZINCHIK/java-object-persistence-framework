package io.github.mizinchik.serialization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

public class JsonSerializer implements Serializer {
    @Override
    public String serialize(Object object) {
        return switch (object) {
            case null -> "null";
            case Number ignored -> object.toString();
            case Character ignored -> object.toString();
            case String string -> "\"" + string + "\"";
            default -> serializeComplex(object);
        };
    }

    private String serializeComplex(Object object) {
        StringBuilder builder = new StringBuilder();
        if (object.getClass().isArray()) {
            builder.append("[");
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                builder.append(serialize(Array.get(object, i))).append(",");
            }
            if (length > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.append("]").toString();
        }
        switch (object) {
            case Collection<?> iterable -> {
                builder.append("[");
                if (!iterable.isEmpty()) {
                    for (Object inner : iterable) {
                        builder.append(serialize(inner)).append(",");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                }
                builder.append("]");
                return builder.toString();
            }
            case Map<?, ?> map -> {
                builder.append("{");
                if (!map.isEmpty()) {
                    for (var entry : map.entrySet()) {
                        builder.append(serialize(entry.getKey())).append(":").append(serialize(entry.getValue())).append(",");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                }
                return builder.append("}").toString();
            }
            default -> {
                builder.append("{");
                serializeFields(object, builder);
                return builder.append("}").toString();
            }
        }
    }

    private void serializeFields(Object object, StringBuilder builder) {
        try {
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isTransient(field.getModifiers())) continue;
                if (field.getAnnotation(Transient.class) != null) continue;
                builder.append("\"").append(field.getName()).append("\":");
                field.setAccessible(true);
                Object fieldValue = field.get(object);
                builder.append(serialize(fieldValue)).append(",");
            }
            if (fields.length > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void serializeToFile(Object object, File file) {
        if (!file.isFile()) {
            throw new IllegalStateException("Not a regular file");
        }
        if (!file.setWritable(true)) {
            throw new IllegalStateException("Couldn't make the file accessible to writing");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.append(serialize(object));
        } catch (IOException e) {
            throw new RuntimeException("Writing to file failed", e);
        }
    }
}
