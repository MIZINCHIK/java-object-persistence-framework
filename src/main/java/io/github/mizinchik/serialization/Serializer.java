package io.github.mizinchik.serialization;

public interface Serializer {
    public String serialize(Object object);
    public void serializeToFile(Object object);
}
