package io.github.mizinchik.persistence.deserialization;

import java.util.Collection;
import java.util.List;

public interface SerialStream<T> {
    SerialStream<T> add(ClassData<T> json);

    SerialStream<T> addAll(Collection<ClassData<T>> jsons);

    List<T> toList();

    List<T> toList(SerialFilter<T> filter);
}
