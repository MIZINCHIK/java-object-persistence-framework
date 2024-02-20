package io.github.mizinchik.serialization;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        TestClass test1 = new TestClass("abc", 1, 0);
        TestClass test2 = new TestClass("def", 12321, 434324);
        TestClass test3 = new TestClass("ghi", -121, 32432);
        String val = "123";
        List<TestClass> list = List.of(test1, test2, test3);
        TestClass[] array = new TestClass[]{new TestClass("abc", 1, 0),
                new TestClass("abc", 1, 0)};
        JsonSerializer serializer = new JsonSerializer();
        System.out.println(serializer.serialize(array));
    }
}