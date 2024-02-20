package io.github.mizinchik.serialization;

import java.io.File;

public interface Serializer {
    public String serialize(Object object);

    public void serializeToFile(Object object, File file);
}
