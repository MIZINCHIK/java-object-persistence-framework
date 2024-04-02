package io.github.mizinchik.persistence.session;

import io.github.mizinchik.persistence.filtering.AttributeFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class JsonSessionTest {
    private static final Path BASE_DIRECTORY = Path.of("src/test/resources");

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

    @BeforeEach
    void createDirectory() throws IOException {
        Files.createDirectories(BASE_DIRECTORY);
    }

    @AfterEach
    void restoreDirectory() {
        try (var walk = Files.walk(BASE_DIRECTORY)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Complex test")
    void persist_whenComplexOperations_thenSuccess() {
        Session session = new JsonSession();
        session.setDirectory(BASE_DIRECTORY);
        var initial = List.of(
                new TestClass2(1),
                new TestClass2(2),
                new TestClass2(3),
                new TestClass2(4),
                new TestClass2(5),
                new TestClass2(6)
        );
        for (var obj : initial) {
            session.insert(obj);
        }
        assertTwoListsEqual(session.find(TestClass2.class), initial);
        session.persist();
        assertTwoListsEqual(session.find(TestClass2.class), initial);
        assertThat(countFiles(TestClass2.class)).isEqualTo(6);
        session.delete(TestClass2.class,
                new AttributeFilter("a", (Integer number) -> number > 1));
        assertTwoListsEqual(session.find(TestClass2.class), List.of(initial.getFirst()));
        session.persist();
        assertThat(countFiles(TestClass2.class)).isEqualTo(1);
        assertTwoListsEqual(session.find(TestClass2.class), List.of(initial.getFirst()));

        var initialComplex = List.of(
                new TestClassComplex(123, "sfdasf", new TestClass2(1)),
                new TestClassComplex(43534, "asdsadsa", new TestClass2(2)),
                new TestClassComplex(321321, "SDsadasd sadasdas", new TestClass2(3)),
                new TestClassComplex(121, "saasd sadasd", new TestClass2(4)),
                new TestClassComplex(1, "a d", new TestClass2(5)),
                new TestClassComplex(0, "sadsadsadsadadasdasdsadsadsadsadsadasd", new TestClass2(6)),
                new TestClassComplex(-1212, "s s s", new TestClass2(7)),
                new TestClassComplex(232, "asdsadsadasdsad", new TestClass2(8)),
                new TestClassComplex(-324235345, "23432423422131231232132132133443", new TestClass2(9)),
                new TestClassComplex(-12121, "asdsadsadsad", new TestClass2(10))
        );
        assertThatThrownBy(() -> countFiles(TestClassComplex.class)).cause().isInstanceOf(NoSuchFileException.class);
        for (var obj : initialComplex) {
            session.insert(obj);
        }
        session.persist();
        assertTwoListsEqual(session.find(TestClassComplex.class), initialComplex);
        session.delete(TestClassComplex.class,
                new AttributeFilter("number", (Integer number) -> number > 0)
                        .and(new AttributeFilter("name", (String name) -> name.contains(" ") || name.length() > 10))
                        .or(new AttributeFilter("testClass2", (TestClass2 testClass2) -> testClass2.a >= 9))
                        .not());
        assertTwoListsEqual(session.find(TestClassComplex.class),
                List.of(
                        initialComplex.get(8),
                        initialComplex.getLast(),
                        initialComplex.get(2),
                        initialComplex.get(3),
                        initialComplex.get(4),
                        initialComplex.get(7)
                ));
        assertThat(countFiles(TestClassComplex.class)).isEqualTo(10);
        session.persist();
        assertThat(countFiles(TestClassComplex.class)).isEqualTo(6);
        assertTwoListsEqual(session.find(TestClassComplex.class,
                        new AttributeFilter(
                                "number", (Integer number) -> number == -12121)),
                List.of(
                        initialComplex.getLast()
                ));
        session.delete(TestClassComplex.class,
                new AttributeFilter("number", (Integer number) -> number != -12121));
        assertThat(countFiles(TestClassComplex.class)).isEqualTo(6);
        session.persist();
        assertThat(countFiles(TestClassComplex.class)).isEqualTo(1);
        assertTwoListsEqual(session.find(TestClassComplex.class,
                        new AttributeFilter(
                                "number", (Integer number) -> number == -12121)),
                List.of(
                        initialComplex.getLast()
                ));
        assertThat(countFiles(TestClassComplex.class)).isEqualTo(1);
        assertThat(countFiles(TestClass2.class)).isEqualTo(1);
    }

    private void assertTwoListsEqual(List list1, List list2) {
        assertThat(list1.size()).isEqualTo(list2.size());
        for (var obj : list2) {
            assertThat(list1.contains(obj)).isTrue();
        }
    }

    private int countFiles(Class clazz) {
        int countFiles = 0;
        try (DirectoryStream<Path> persisted =
                     Files.newDirectoryStream(
                             BASE_DIRECTORY.resolve(clazz.getName()),
                             "*.jvivo")) {
            for (var ignored : persisted) {
                countFiles++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return countFiles;
    }
}
