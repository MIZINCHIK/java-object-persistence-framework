package io.github.mizinchik.persistence.serialization;


import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonSerialStreamTest {
    private static class TestClass {
        private char a;
        private byte b;
        private short c;

        public TestClass() {
        }

        public TestClass(char a, byte b, short c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClass that = (TestClass) o;
            return a == that.a && b == that.b && c == that.c;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c);
        }
    }

    @ParameterizedTest
    @CsvSource({"c,0,12172", "0,0,0", "糞,127,127"})
    @DisplayName("Deserializing simple classes with primitive fields")
    void serialize_whenClassWithPrimitives_thenCorrect(char a, byte b, short c) {
        var stream = new JsonSerialStream<>(new JSONObject("{\"a\":\"" + a + "\",\"b\":" + b + ",\"c\":" + c + "}"), TestClass.class);
        assertThat(stream.instance()).isEqualTo(new TestClass(a, b, c));
    }

    private static class TestClass2 {
        private int a;

        public TestClass2(int a) {
            this.a = a;
        }

        public TestClass2() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClass2 testClass2 = (TestClass2) o;
            return a == testClass2.a;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a);
        }
    }

    private static class TestClassComplex {
        private Integer number;
        private String name;
        private TestClass2 testClass2;

        public TestClassComplex(Integer number, String name, TestClass2 testClass2) {
            this.number = number;
            this.name = name;
            this.testClass2 = testClass2;
        }

        public TestClassComplex() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClassComplex that = (TestClassComplex) o;
            return Objects.equals(number, that.number) && Objects.equals(name, that.name) && Objects.equals(testClass2, that.testClass2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number, name, testClass2);
        }
    }

    @ParameterizedTest
    @MethodSource("provideArgs_serialize_whenClassWithComplexFields_thenCorrect")
    @DisplayName("Deserializing classes with complex fields")
    void serialize_whenClassWithComplexFields_thenCorrect(Integer number, String name, int number2) {
        var stream = new JsonSerialStream<>(new JSONObject("{\"number\":" + number + ",\"name\":" +
                (name == null ? "null" : "\"" + name + "\"") + ",\"testClass2\":{\"a\":" + number2 + "}}"), TestClassComplex.class);
        assertThat(stream.instance()).usingRecursiveComparison().isEqualTo(new TestClassComplex(number, name, new TestClass2(number2)));
    }

    private static Stream<Arguments> provideArgs_serialize_whenClassWithComplexFields_thenCorrect() {
        return Stream.of(
                Arguments.of(0, "asdiujoadj", 1223),
                Arguments.of(null, "121312", 213213),
                Arguments.of(null, null, 0)
        );
    }
}
