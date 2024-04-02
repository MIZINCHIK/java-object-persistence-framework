package io.github.mizinchik.persistence.session;

import io.github.mizinchik.persistence.deserialization.Deserializer;
import io.github.mizinchik.persistence.deserialization.JsonDeserializer;
import io.github.mizinchik.persistence.deserialization.JsonSerialStream;
import io.github.mizinchik.persistence.deserialization.SerialStream;
import io.github.mizinchik.persistence.filtering.AttributeFilter;
import io.github.mizinchik.persistence.logging.Banner;
import io.github.mizinchik.persistence.serialization.JsonSerializer;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import static java.util.UUID.randomUUID;

public class JsonSession implements Session {
    private static final String SUBDIRECTORY = "jvivo";
    private static final String GLOB = "*.jvivo";
    private static final String EXTENSION = ".jvivo";
    private static final Banner BANNER = new Banner();
    private final Map<Class, AttributeFilter> deleteFilters;
    private final IdentityHashMap<Class, Set<String>> dumps;
    private final JsonSerializer jsonSerializer;
    private Path directory;

    public JsonSession() {
        directory = Path.of(String.valueOf(this.getClass().getClassLoader().getResource(""))).resolve(SUBDIRECTORY);
        dumps = new IdentityHashMap<>();
        deleteFilters = new HashMap<>();
        jsonSerializer = new JsonSerializer();
    }

    @Override
    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    @Override
    public <T> void insert(T object) {
        Set<String> currentDump = dumps.getOrDefault(object.getClass(), new HashSet<>());
        currentDump.add(jsonSerializer.serialize(object));
        dumps.put(object.getClass(), currentDump);
    }

    @Override
    public <T> List<T> find(Class<T> clazz) {
        if (deleteFilters.containsKey(clazz)) {
            return obtainStream(clazz).toListExclude(deleteFilters.get(clazz));
        } else {
            return obtainStream(clazz).toList();
        }
    }

    @Override
    public <T> List<T> find(Class<T> clazz, AttributeFilter filter) {
        persist();
        return obtainStream(clazz).toList(filter);
    }

    private <T> JsonSerialStream<T> obtainStream(Class<T> clazz) {
        JsonSerialStream<T> serialStream = new JsonSerialStream<>(clazz);
        if (dumps.containsKey(clazz)) {
            serialStream.addAll(dumps.get(clazz));
        }
        try (DirectoryStream<Path> persisted = Files.newDirectoryStream(directory.resolve(clazz.getName()), GLOB)) {
            for (Path file : persisted) {
                serialStream.add(file.toFile());
            }
        } catch (IOException ignored) {
        }
        return serialStream;
    }

    @Override
    public <T> void delete(Class<T> clazz, AttributeFilter filter) {
        if (deleteFilters.containsKey(clazz)) {
            deleteFilters.put(clazz, deleteFilters.get(clazz).or(filter));
        } else {
            deleteFilters.put(clazz, filter);
        }
    }


    @Override
    public void persist() {
        for (var clazz : dumps.keySet()) {
            Path classDirectory = directory.resolve(clazz.getName());
            try {
                Files.createDirectories(classDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for (var obj : dumps.get(clazz)) {
                try {
                    Path newFile = classDirectory.resolve(randomUUID() + EXTENSION);
                    Files.createFile(newFile);
                    Files.writeString(newFile, obj);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        dumps.clear();
        for (var clazz : deleteFilters.keySet()) {
            Path classDirectory = directory.resolve(clazz.getName());
            try {
                Files.createDirectories(classDirectory);
            } catch (IOException e) {
                return;
            }
            AttributeFilter filter = deleteFilters.get(clazz);
            try (DirectoryStream<Path> persisted = Files.newDirectoryStream(classDirectory, GLOB)) {
                for (Path file : persisted) {
                    if (filter.evaluate(new JsonDeserializer<>(
                            new JSONObject(Files.readString(file)),
                            clazz)
                            .getRelevantFields(filter.getNecessaryFields()))) {
                        Files.delete(file);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public <T> Deserializer<T> getDeserializer(Object jsonObject, Class<T> clazz) {
        return new JsonDeserializer<>(jsonObject, clazz);
    }

    @Override
    public <T> Deserializer<T> getDeserializer(JSONObject jsonObject, Class<T> clazz) {
        return new JsonDeserializer<>(jsonObject, clazz);
    }

    @Override
    public <T> SerialStream<T> getSerializer(Class<T> clazz) {
        return new JsonSerialStream<>(clazz);
    }
}
