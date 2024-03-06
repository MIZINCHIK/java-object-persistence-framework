package io.github.mizinchik.persistence.serialization;

import java.io.File;

public interface Serializer {
    String serialize(Object object);

    void serialize(Object object, File file);
}
