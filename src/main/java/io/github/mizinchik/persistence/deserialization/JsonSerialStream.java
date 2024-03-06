package io.github.mizinchik.persistence.deserialization;


import io.github.mizinchik.persistence.annotations.Transient;
import io.github.mizinchik.persistence.exceptions.EmptyObjectInstantiationFailedException;
import io.github.mizinchik.persistence.exceptions.FailedFieldAccessException;
import io.github.mizinchik.persistence.exceptions.NoArgumentConstructorMissingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonSerialStream<T> implements SerialStream<T> {
    private final JSONObject json;
    private final Class<T> clazz;

    public JsonSerialStream(JSONObject json, Class<T> clazz) {
        this.json = json;
        this.clazz = clazz;
    }

    @Override
    public T instance() {
        try {
            T prototype = clazz.getConstructor().newInstance();
            List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                    .filter(JsonSerialStream::isEligibleField)
                    .toList();
            for (var field : fields) {
                field.setAccessible(true);
                setField(field, prototype);
            }
            return prototype;
        } catch (NoSuchMethodException e) {
            throw new NoArgumentConstructorMissingException(e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new EmptyObjectInstantiationFailedException(e);
        }
    }

    private void setField(Field field, T prototype) {
        Class<?> fieldType = field.getType();
        String fieldName = field.getName();
        try {
            if (fieldType.isPrimitive()) {
                if (fieldType == int.class) {
                    field.setInt(prototype, json.getInt(fieldName));
                } else if (fieldType == boolean.class) {
                    field.setBoolean(prototype, json.getBoolean(fieldName));
                } else if (fieldType == short.class) {
                    field.setShort(prototype, (short) json.getInt(fieldName));
                } else if (fieldType == char.class) {
                    String string = json.getString(fieldName);
                    if (string.length() != 1) {
                        throw new RuntimeException("Incorrect char format");
                    }
                    field.setChar(prototype, string.charAt(0));
                } else if (fieldType == double.class) {
                    field.setDouble(prototype, json.getDouble(fieldName));
                } else if (fieldType == float.class) {
                    field.setFloat(prototype, json.getFloat(fieldName));
                } else if (fieldType == byte.class) {
                    field.setByte(prototype, (byte) json.getInt(fieldName));
                } else if (fieldType == long.class) {
                    field.setLong(prototype, json.getLong(fieldName));
                } else {
                    throw new RuntimeException("Unknown primitive");
                }
            } else {
                setField(json.get(field.getName()), field, prototype);
            }
        } catch (IllegalAccessException e) {
            throw new FailedFieldAccessException(e);
        }
    }

    private void setField(Object object, Field field, T prototype) throws IllegalAccessException {
        if (object == JSONObject.NULL) {
            field.set(prototype, null);
            return;
        }
        switch (object) {
            case JSONObject jsonObject ->
                    setField(new JsonSerialStream<>(jsonObject, field.getType()).instance(), field, prototype);
            case JSONArray objects -> {
                //TODO
            }
            default -> {
                try {
                    field.set(prototype, object);
                } catch (IllegalAccessException e) {
                    throw new FailedFieldAccessException(e);
                }
            }
        }
    }

    @Override
    public List<T> list() {
        return null;
    }

    @Override
    public <K> Map<K, T> map(Class<K> keyClazz) {
        return null;
    }

    private static boolean isEligibleField(Field field) {
        int modifiers = field.getModifiers();
        return !field.isAnnotationPresent(Transient.class)
                && !Modifier.isTransient(modifiers)
                && !Modifier.isFinal(modifiers);
    }
}
