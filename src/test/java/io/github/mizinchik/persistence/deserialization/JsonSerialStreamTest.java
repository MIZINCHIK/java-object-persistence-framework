package io.github.mizinchik.persistence.deserialization;

import io.github.mizinchik.persistence.serialization.JsonSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class JsonSerialStreamTest {
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
            return Objects.equals(number, that.number) && Objects.equals(name, that.name)
                    && Objects.equals(testClass2, that.testClass2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number, name, testClass2);
        }
    }

    @Test
    @DisplayName("Complex filtering test")
    void toList_whenFiltered_thenOnlyValid() {
        JsonSerialStream<TestClassComplex> stream = new JsonSerialStream<>(TestClassComplex.class);
        List<TestClassComplex> testClassComplexesData = List.of(
                new TestClassComplex(1, "one", new TestClass2(9)),
                new TestClassComplex(2, "asdasdfdeas", new TestClass2(8)),
                new TestClassComplex(3, "dsfhsdjkghrujegvuieghvurefgvuregbuhivrgehvrhbeujv", new TestClass2(7)),
                new TestClassComplex(4, "ooga", new TestClass2(6)),
                new TestClassComplex(5, "booga", new TestClass2(5)),
                new TestClassComplex(6, "random_string", new TestClass2(4)),
                new TestClassComplex(7, "hellow world!", new TestClass2(3)),
                new TestClassComplex(8, "hippity hoppity", new TestClass2(2))
        );
        JsonSerializer serializer = new JsonSerializer();
        stream.addAll(testClassComplexesData
                .stream()
                .map(serializer::serialize)
                .toList());
        JsonSerialFilter filter = new JsonSerialFilter(
                "number", (Integer number) -> number < 2)
                .negate("number")
                .and("name", (String name) -> name.length() > 20)
                .or("name", (String name) -> name.contains(" "));
        List<TestClassComplex> filteredData = stream.toList(filter);
        assertThat(filteredData).usingRecursiveComparison().isEqualTo(
                List.of(testClassComplexesData.get(2), testClassComplexesData.get(6), testClassComplexesData.get(7))
        );
    }

    @Test
    @DisplayName("Incorrect type of an attribute")
    void toList_whenFilteredWithWronAttributeTypes_thenTreatedAsFalse() {
        JsonSerialStream<TestClassComplex> stream = new JsonSerialStream<>(TestClassComplex.class);
        List<TestClassComplex> testClassComplexesData = List.of(
                new TestClassComplex(1, "one", new TestClass2(9)),
                new TestClassComplex(2, "asdasdfdeas", new TestClass2(8)),
                new TestClassComplex(3, "dsfhsdjkghrujegvuieghvurefgvuregbuhivrgehvrhbeujv", new TestClass2(7)),
                new TestClassComplex(4, "ooga", new TestClass2(6)),
                new TestClassComplex(5, "booga", new TestClass2(5)),
                new TestClassComplex(6, "random_string", new TestClass2(4)),
                new TestClassComplex(7, "hellow world!", new TestClass2(3)),
                new TestClassComplex(8, "hippity hoppity", new TestClass2(2))
        );
        JsonSerializer serializer = new JsonSerializer();
        stream.addAll(testClassComplexesData
                .stream()
                .map(serializer::serialize)
                .toList());
        JsonSerialFilter filter = new JsonSerialFilter(
                "number", (Integer number) -> number < 2)
                .negate("number")
                .and("name", (String name) -> name.length() > 20)
                .or("name", (Integer name) -> name > 3);
        List<TestClassComplex> filteredData = stream.toList(filter);
        assertThat(filteredData).usingRecursiveComparison().isEqualTo(
                List.of(testClassComplexesData.get(2))
        );
    }
}
