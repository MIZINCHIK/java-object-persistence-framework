package io.github.mizinchik.persistence.deserialization;

import java.util.List;
import java.util.Map;

public interface SerialStream<T> {
    T instance();

    List<T> list();

    <K> Map<K, T> map(Class<K> keyClazz);
}
