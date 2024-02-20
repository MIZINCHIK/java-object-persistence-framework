package io.github.mizinchik.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonSerializerTest {
    private Serializer serializer;

    @BeforeEach
    void init() {
        serializer = new JsonSerializer();
    }

    @Test
    @DisplayName("Serializing primitives")
    void serialize_whenPrimitive_thenCorrect() {
        assertThat(serializer.serialize(1)).isEqualTo("1");
        assertThat(serializer.serialize(1L)).isEqualTo("1");
        assertThat(serializer.serialize(1D)).isEqualTo("1.0");
        assertThat(serializer.serialize(1.02130213)).isEqualTo("1.02130213");
        assertThat(serializer.serialize('a')).isEqualTo("a");
    }

    @Test
    @DisplayName("Serializing nulls")
    void serialize_whenNull_thenCorrect() {
        assertThat(serializer.serialize(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("Serializing Strings")
    void serialize_whenString_thenCorrect() {
        assertThat(serializer.serialize("sadghjasdhjsadhjsad")).isEqualTo("\"sadghjasdhjsadhjsad\"");
        assertThat(serializer.serialize("asdsadfafwwefewfewscewce")).isEqualTo("\"asdsadfafwwefewfewscewce\"");
        assertThat(serializer.serialize("asdsadsadasdsadsadsadadsdsa\"sdsadsadasdasdsadsa")).isEqualTo("\"asdsadsadasdsadsadsadadsdsa\"sdsadsadasdasdsadsa\"");
        assertThat(serializer.serialize("null")).isEqualTo("\"null\"");
    }

    private class TestClass {
        private Integer number1;
        private String name;
        private int number2;

        public TestClass(Integer number1, String name, int number2) {
            this.number1 = number1;
            this.name = name;
            this.number2 = number2;
        }
    }

    @Test
    @DisplayName("Serializing classes w/o links to others of their lot")
    void serialize_whenBasicClasses_thenCorrect() {
        assertThat(serializer.serialize(new TestClass(1, "sdasd", 2))).isEqualTo("{\"number1\":1,\"name\":\"sdasd\",\"number2\":2}");
        assertThat(serializer.serialize(new TestClass(null, null, 2))).isEqualTo("{\"number1\":null,\"name\":null,\"number2\":2}");
    }

    private record TestRecord(Integer number1, String name, int number2) {
    }

    @Test
    @DisplayName("Serializing records w/o links to others of their lot")
    void serialize_whenBasicRecords_thenCorrect() {
        assertThat(serializer.serialize(new TestRecord(1, "sdasd", 2))).isEqualTo("{\"number1\":1,\"name\":\"sdasd\",\"number2\":2}");
        assertThat(serializer.serialize(new TestRecord(null, null, 2))).isEqualTo("{\"number1\":null,\"name\":null,\"number2\":2}");
    }

    @Test
    @DisplayName("Serializing arrays")
    void serialize_whenArray_thenCorrect() {
        assertThat(serializer.serialize(new int[]{1, 2, 3, 4})).isEqualTo("[1,2,3,4]");
        assertThat(serializer.serialize(new String[]{null, "null", "absbdhbasdas"})).isEqualTo("[null,\"null\",\"absbdhbasdas\"]");
    }

    @Test
    @DisplayName("Serializing collections")
    void serialize_whenCollection_thenCorrect() {
        assertThat(serializer.serialize(List.of(1, 2, 3, 4))).isEqualTo("[1,2,3,4]");
        assertThat(serializer.serialize(List.of("null", "absbdhbasdas"))).isEqualTo("[\"null\",\"absbdhbasdas\"]");
    }
}
