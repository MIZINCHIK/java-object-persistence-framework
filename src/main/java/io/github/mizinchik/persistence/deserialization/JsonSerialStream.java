package io.github.mizinchik.persistence.deserialization;


import io.github.mizinchik.persistence.annotations.Transient;
import io.github.mizinchik.persistence.exceptions.EmptyObjectInstantiationFailedException;
import io.github.mizinchik.persistence.exceptions.FailedCollectionDeserializationException;
import io.github.mizinchik.persistence.exceptions.FailedFieldAccessException;
import io.github.mizinchik.persistence.exceptions.FailedMapDeserializationException;
import io.github.mizinchik.persistence.exceptions.NoArgumentConstructorMissingException;
import io.github.mizinchik.persistence.exceptions.ParametrizedTypeDeserializationException;
import io.github.mizinchik.persistence.exceptions.UnsupportedParametrizedCollection;
import io.github.mizinchik.persistence.exceptions.UnsupportedParametrizedMap;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonSerialStream<T> implements SerialStream<T> {
    private ArrayList<?> arrayList;
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
        } else if (Map.class.isAssignableFrom(field.getType())) {
            setMapField((JSONObject) object, field, prototype);
        } else {
            switch (object) {
                case JSONObject jsonObject ->
                        setField(new JsonSerialStream<>(jsonObject, field.getType()).instance(), field, prototype);
                case JSONArray objects -> {
                    Class<?> type = field.getType();
                    if (Collection.class.isAssignableFrom(type)) {
                        setCollectionField(objects, field, prototype);
                    } else if (type.isArray()) {
                        field.set(prototype, getArray(objects, field.getType().getComponentType()));
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

    @SuppressWarnings("CyclomaticComplexity")
    private Collection<?> getCollection(Type collectionGenericType, Type elementType, JSONArray array) {
        Collection collection = instantiateCollection(((ParameterizedType) collectionGenericType).getRawType());
        if (elementType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (Collection.class.isAssignableFrom((Class) elementType)) {
                if (typeArgs.length != 1) {
                    throw new UnsupportedParametrizedCollection();
                }
                for (var arrayElement : array) {
                    collection.add(getCollection(elementType, typeArgs[0], (JSONArray) arrayElement));
                }
            } else if (Map.class.isAssignableFrom(((Class) elementType))) {
                if (typeArgs.length != 2) {
                    throw new UnsupportedParametrizedMap();
                }
                for (var arrayElement : array) {
                    collection.add(getMap(elementType, typeArgs[0], typeArgs[1], (JSONObject) arrayElement));
                }
            }
        } else {
            int length = array.length();
            Function<Integer, ?> elementSupplier;
            Class elementClass = (Class) elementType;
            Consumer filler = (obj) -> collection.add(obj);
            if (elementClass == Integer.class || elementClass == int.class) {
                elementSupplier = array::getInt;
            } else if (elementClass == Double.class || elementClass == double.class) {
                elementSupplier = array::getDouble;
            } else if (elementClass == Long.class || elementClass == long.class) {
                elementSupplier = array::getLong;
            } else if (elementClass == Character.class || elementClass == char.class) {
                elementSupplier = (index) -> {
                    String defaultString = array.getString(index);
                    return defaultString == null || defaultString.length() != 1 ? null : defaultString.charAt(0);
                };
            } else if (elementClass == String.class) {
                elementSupplier = array::getString;
            } else if (elementClass == Byte.class || elementClass == byte.class) {
                elementSupplier = (index) -> (byte) array.getNumber(index);
            } else if (elementClass == Float.class || elementClass == float.class) {
                elementSupplier = array::getFloat;
            } else if (elementClass == Short.class || elementClass == short.class) {
                elementSupplier = (index) -> (short) array.getNumber(index);
            } else if (elementClass.isArray()) {
                elementSupplier = array::getJSONArray;
                filler = (obj) -> collection.add(getArray((JSONArray) obj, elementClass.getComponentType()));
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

    private Object getArray(JSONArray array, Class componentType) {
        try {
            ArrayList arrayListToGet = (ArrayList) getCollection(
                    this.getClass().getDeclaredField("arrayList").getGenericType(),
                    componentType,
                    array);
            Object result;
            if (componentType == int.class) {
                result = arrayListToGet.stream().mapToInt(i -> (int) i).toArray();
            } else if (componentType == long.class) {
                result = arrayListToGet.stream().mapToLong(i -> (long) i).toArray();
            } else if (componentType == double.class) {
                result = arrayListToGet.stream().mapToDouble(i -> (double) i).toArray();
            } else if (componentType == Integer.class) {
                result = arrayListToGet.toArray(new Integer[arrayListToGet.size()]);
            } else if (componentType == Long.class) {
                result = arrayListToGet.toArray(new Long[arrayListToGet.size()]);
            } else if (componentType == Short.class) {
                result = arrayListToGet.toArray(new Short[arrayListToGet.size()]);
            } else if (componentType == Byte.class) {
                result = arrayListToGet.toArray(new Byte[arrayListToGet.size()]);
            } else if (componentType == Boolean.class) {
                result = arrayListToGet.toArray(new Boolean[arrayListToGet.size()]);
            } else if (componentType == Double.class) {
                result = arrayListToGet.toArray(new Double[arrayListToGet.size()]);
            } else if (componentType == Float.class) {
                result = arrayListToGet.toArray(new Float[arrayListToGet.size()]);
            } else if (componentType == Character.class) {
                result = arrayListToGet.toArray(new Character[arrayListToGet.size()]);
            } else if (componentType == String.class) {
                result = arrayListToGet.toArray(new String[arrayListToGet.size()]);
            } else {
                result = null;
            }
            return result;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
//
//    private void setArrayField(JSONArray array, Field field, T prototype) {
//        try {
//            Class componentType = field.getType().getComponentType();
//            ArrayList arrayList = (ArrayList) getCollection(
//                    this.getClass().getDeclaredField("arrayList").getGenericType(),
//                    componentType,
//                    array);
//            if (componentType == int.class) {
//                field.set(prototype, arrayList.stream().mapToInt(i -> (int) i).toArray());
//            } else if (componentType == long.class) {
//                field.set(prototype, arrayList.stream().mapToLong(i -> (long) i).toArray());
//            } else if (componentType == double.class) {
//                field.set(prototype, arrayList.stream().mapToDouble(i -> (double) i).toArray());
//            } else if (componentType == Integer.class) {
//                field.set(prototype, arrayList.toArray(new Integer[arrayList.size()]));
//            } else if (componentType == Long.class) {
//                field.set(prototype, arrayList.toArray(new Long[arrayList.size()]));
//            } else if (componentType == Short.class) {
//                field.set(prototype, arrayList.toArray(new Short[arrayList.size()]));
//            } else if (componentType == Byte.class) {
//                field.set(prototype, arrayList.toArray(new Byte[arrayList.size()]));
//            } else if (componentType == Boolean.class) {
//                field.set(prototype, arrayList.toArray(new Boolean[arrayList.size()]));
//            } else if (componentType == Double.class) {
//                field.set(prototype, arrayList.toArray(new Double[arrayList.size()]));
//            } else if (componentType == Float.class) {
//                field.set(prototype, arrayList.toArray(new Float[arrayList.size()]));
//            } else if (componentType == Character.class) {
//                field.set(prototype, arrayList.toArray(new Character[arrayList.size()]));
//            } else if (componentType == String.class) {
//                field.set(prototype, arrayList.toArray(new String[arrayList.size()]));
//            }
//        } catch (IllegalAccessException e) {
//            throw new FailedCollectionDeserializationException(e);
//        } catch (NoSuchFieldException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private void setMapField(JSONObject map, Field field, T prototype) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length != 2) {
                throw new UnsupportedParametrizedMap();
            }
            try {
                field.set(prototype, getMap(genericType, typeArgs[0], typeArgs[1], map));
            } catch (IllegalAccessException e) {
                throw new FailedCollectionDeserializationException(e);
            }
        }
    }

    private Map<?, ?> getMap(Type mapGenericType, Type keyType, Type valueType, JSONObject object) {
        Map map = instantiateMap(((ParameterizedType) mapGenericType).getRawType());
        for (var key : object.keySet()) {
            map.put(getObjectFromString(key, keyType), getObjectFromString(object.optString(key), valueType));
        }
        return map;
    }

    private Map instantiateMap(Type rawType) {
        Class<? extends Map> clzz = (Class) rawType;
        if ((clzz).isInterface()) {
            if (clzz == Map.class) {
                return new HashMap();
            } else if (clzz == SortedMap.class) {
                return new TreeMap();
            } else {
                throw new FailedMapDeserializationException();
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

    private Object getObjectFromString(String value, Type type) {
        if (value.equals("null")) {
            return null;
        }
        Object result;
        if (type instanceof ParameterizedType parameterizedType) {
            Class rawType = (Class) parameterizedType.getRawType();
            if (Collection.class.isAssignableFrom(rawType)) {
                result = getCollection(type, ((ParameterizedType) type)
                        .getActualTypeArguments()[0], new JSONArray(value));
            } else if (Map.class.isAssignableFrom(rawType)) {
                result = getMap(type, ((ParameterizedType) type)
                        .getActualTypeArguments()[0], ((ParameterizedType) type)
                        .getActualTypeArguments()[1], new JSONObject(value));
            } else {
                result = new JsonSerialStream(new JSONObject(value), rawType).instance();
            }
        } else {
            Class clzz = (Class) type;
            if (type == Integer.class) {
                result = Integer.parseInt(value);
            } else if (clzz == Long.class) {
                result = Long.parseLong(value);
            } else if (clzz == Short.class) {
                result = Short.parseShort(value);
            } else if (clzz == Double.class) {
                result = Double.parseDouble(value);
            } else if (clzz == Float.class) {
                result = Float.parseFloat(value);
            } else if (clzz == Byte.class) {
                result = Byte.parseByte(value);
            } else if (clzz == Character.class) {
                if (value.length() != 1) {
                    throw new RuntimeException("Incorrect char format");
                }
                result = value.charAt(0);
            } else if (clzz == Boolean.class) {
                result = Boolean.parseBoolean(value);
            } else if (clzz == String.class) {
                result = value;
            } else if (clzz.isArray()) {
                result = getArray(new JSONArray(value), ((Class<?>) type).getComponentType());
            } else {
                result = new JsonSerialStream(new JSONObject(value), clzz).instance();
            }
        }
        return result;
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
