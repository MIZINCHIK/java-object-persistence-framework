package io.github.mizinchik.persistence.deserialization;

import java.io.File;
import java.util.Collection;
import java.util.List;

public interface SerialStream<T> {
    SerialStream<T> add(String json);

    SerialStream<T> addAll(Collection<String> jsons);

    SerialStream<T> add(File json);

    SerialStream<T> addAllFiles(Collection<File> jsons);

    List<T> toList();

    List<T> toList(SerialFilter filter);
}
