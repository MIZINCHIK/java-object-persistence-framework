package io.github.mizinchik.persistence.deserialization;

import java.util.Collection;
import java.util.Map;

public interface Deserializer<T> {
    T instance();

    Collection<T> collection();

    <K> Map<K, T> map(Class<K> keyClazz);
}
