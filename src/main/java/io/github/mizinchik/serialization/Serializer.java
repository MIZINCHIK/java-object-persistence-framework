package io.github.mizinchik.serialization;

import java.io.File;

public interface Serializer {
    String serialize(Object object);

    void serializeToFile(Object object, File file);
}
