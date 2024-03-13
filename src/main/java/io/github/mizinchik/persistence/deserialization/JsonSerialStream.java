package io.github.mizinchik.persistence.deserialization;


import io.github.mizinchik.persistence.annotations.Transient;
import io.github.mizinchik.persistence.exceptions.EmptyObjectInstantiationFailedException;
import io.github.mizinchik.persistence.exceptions.FailedCollectionDeserializationException;
import io.github.mizinchik.persistence.exceptions.FailedFieldAccessException;
import io.github.mizinchik.persistence.exceptions.NoArgumentConstructorMissingException;
import io.github.mizinchik.persistence.exceptions.ParametrizedTypeDeserializationException;
import io.github.mizinchik.persistence.exceptions.UnsupportedParametrizedCollection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonSerialStream<T> implements SerialStream<T> {
    private final JSONObject json;
    private final Class<T> clazz;

    public JsonSerialStream(JSONObject json, Class<T> clazz) {
        this.json = json;
        if (clazz.getTypeParameters().length > 0) {
            throw new ParametrizedTypeDeserializationException();
        }
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

    @SuppressWarnings({"MultipleStringLiterals", "CyclomaticComplexity"})
    private void setField(Field field, T prototype) {
        Class<?> fieldType = field.getType();
        String fieldName = field.getName();
        try {
            if (fieldType.isPrimitive() || fieldType == Character.class) {
                if (fieldType == int.class) {
                    field.setInt(prototype, json.getInt(fieldName));
                } else if (fieldType == boolean.class) {
                    field.setBoolean(prototype, json.getBoolean(fieldName));
                } else if (fieldType == short.class) {
                    field.setShort(prototype, (short) json.getInt(fieldName));
                } else if (fieldType == char.class || fieldType == Character.class) {
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
            } else if (Number.class.isAssignableFrom(fieldType)) {
                if (json.get(fieldName) == JSONObject.NULL) {
                    field.set(prototype, null);
                } else {
                    if (fieldType == Integer.class) {
                        field.set(prototype, json.get(fieldName));
                    } else if (fieldType == Boolean.class) {
                        field.set(prototype, json.getBoolean(fieldName));
                    } else if (fieldType == Short.class) {
                        field.set(prototype, (short) json.getInt(fieldName));
                    } else if (fieldType == Double.class) {
                        field.set(prototype, json.getDouble(fieldName));
                    } else if (fieldType == Float.class) {
                        field.set(prototype, json.getFloat(fieldName));
                    } else if (fieldType == Byte.class) {
                        field.set(prototype, (byte) json.getInt(fieldName));
                    } else if (fieldType == Long.class) {
                        field.set(prototype, json.getLong(fieldName));
                    } else {
                        throw new RuntimeException("Unknown primitive");
                    }
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
                Class<?> type = field.getType();
                if (Collection.class.isAssignableFrom(type)) {
                    setCollectionField(objects, field, prototype);
                } else if (type.isArray()) {
                    setArrayField(objects, field, prototype);
                }
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

    private void setCollectionField(JSONArray array, Field field, T prototype) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length != 1) {
                throw new UnsupportedParametrizedCollection();
            }
            try {
                field.set(prototype, getCollection(genericType, typeArgs[0], array));
            } catch (IllegalAccessException e) {
                throw new FailedCollectionDeserializationException(e);
            }
        }
    }

    private Collection<?> getCollection(Type collectionGenericType, Type elementType, JSONArray array) {
        Collection collection = instantiateCollection(((ParameterizedType) collectionGenericType).getRawType());
        if (elementType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length != 1) {
                throw new UnsupportedParametrizedCollection();
            }
            if ((Collection.class.isAssignableFrom((Class) elementType))) {
                for (var arrayElement : array) {
                    collection.add(getCollection(elementType, typeArgs[0], (JSONArray) arrayElement));
                }
            } else {
                // TODO
            }
        } else {
            int length = array.length();
            Function<Integer, ?> elementSupplier;
            Class elementClass = (Class) elementType;
            Consumer filler = (obj) -> collection.add(obj);
            if (elementClass == Integer.class) {
                elementSupplier = array::getInt;
            } else if (elementClass == Double.class) {
                elementSupplier = array::getDouble;
            } else if (elementClass == Long.class) {
                elementSupplier = array::getLong;
            } else if (elementClass == Character.class) {
                elementSupplier = (index) -> {
                    String defaultString = array.getString(index);
                    return defaultString == null || defaultString.length() != 1 ? null : defaultString.charAt(0);
                };
            } else if (elementClass == String.class) {
                elementSupplier = array::getString;
            } else if (elementClass == Byte.class) {
                elementSupplier = (index) -> (byte) array.getNumber(index);
            } else if (elementClass == Float.class) {
                elementSupplier = array::getFloat;
            } else if (elementClass == Short.class) {
                elementSupplier = (index) -> (short) array.getNumber(index);
            } else {
                elementSupplier = array::getJSONObject;
                filler = (obj) -> collection.add(getCollectionElement((JSONObject) obj, elementType));
            }
            for (int i = 0; i < length; i++) {
                filler.accept(elementSupplier.apply(i));
            }
        }
        return collection;
    }

    private Collection instantiateCollection(Type rawType) {
        Class<? extends Collection> clzz = (Class) rawType;
        if ((clzz).isInterface()) {
            if (List.class.isAssignableFrom(clzz)) {
                return new ArrayList<>();
            } else if (Set.class.isAssignableFrom(clzz)) {
                return new HashSet();
            } else {
                throw new FailedCollectionDeserializationException();
            }
        } else {
            try {
                return clzz.getConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                throw new NoArgumentConstructorMissingException(e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new EmptyObjectInstantiationFailedException(e);
            }
        }
    }

    private Object getCollectionElement(JSONObject arrayElement, Type singleType) {
        return new JsonSerialStream<>(arrayElement, (Class<?>) singleType).instance();
    }

    private void setArrayField(JSONArray array, Field field, T prototype) {
        //TODO
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
