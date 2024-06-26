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
import java.lang.reflect.Constructor;
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

public class JsonDeserializer<T> implements Deserializer<T> {
    private ArrayList<?> arrayList;
    private final Object json;
    private JSONObject jsonObject;
    private final Class<T> clazz;

    public JsonDeserializer(Object json, Class<T> clazz) {
        this.json = json;
        if (clazz.getTypeParameters().length > 0) {
            throw new ParametrizedTypeDeserializationException();
        }
        this.clazz = clazz;
    }

    public JsonDeserializer(JSONObject json, Class<T> clazz) {
        this.json = json;
        this.jsonObject = json;
        if (clazz.getTypeParameters().length > 0) {
            throw new ParametrizedTypeDeserializationException();
        }
        this.clazz = clazz;
    }

    public Map<String, Object> getRelevantFields(Set<String> fieldNames) {
        var result = new HashMap<String, Object>();
        for (var fieldName : fieldNames) {
            result.put(fieldName, getField(fieldName));
        }
        return result;
    }

    @SuppressWarnings("CyclomaticComplexity")
    public Object getField(String fieldName) {
        Object result = null;
        try {
            Class targetFieldType = clazz.getDeclaredField(fieldName).getType();
            if (targetFieldType.isPrimitive()
                    || targetFieldType == Character.class
                    || Number.class.isAssignableFrom(targetFieldType)) {
                if (targetFieldType == int.class || targetFieldType == Integer.class) {
                    result = jsonObject.getInt(fieldName);
                } else if (targetFieldType == boolean.class || targetFieldType == Boolean.class) {
                    result = jsonObject.getBoolean(fieldName);
                } else if (targetFieldType == short.class || targetFieldType == Short.class) {
                    result = (short) jsonObject.getInt(fieldName);
                } else if (targetFieldType == char.class || targetFieldType == Character.class) {
                    String string = jsonObject.getString(fieldName);
                    if (string.length() == 1) {
                        result = string.charAt(0);
                    }
                } else if (targetFieldType == double.class || targetFieldType == Double.class) {
                    result = jsonObject.getDouble(fieldName);
                } else if (targetFieldType == float.class || targetFieldType == Float.class) {
                    result = jsonObject.getFloat(fieldName);
                } else if (targetFieldType == byte.class || targetFieldType == Byte.class) {
                    result = (byte) jsonObject.getInt(fieldName);
                } else if (targetFieldType == long.class || targetFieldType == Long.class) {
                    result = jsonObject.getLong(fieldName);
                }
            } else {
                try {
                    result = getField(jsonObject.get(fieldName), clazz.getDeclaredField(fieldName));
                } catch (Exception ignoredAgain) {
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    @Override
    public T instance() {
        try {
            jsonObject = (JSONObject) json;
            Constructor<T> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            T prototype = constructor.newInstance();
            List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                    .filter(JsonDeserializer::isEligibleField)
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
                    field.setInt(prototype, jsonObject.getInt(fieldName));
                } else if (fieldType == boolean.class) {
                    field.setBoolean(prototype, jsonObject.getBoolean(fieldName));
                } else if (fieldType == short.class) {
                    field.setShort(prototype, (short) jsonObject.getInt(fieldName));
                } else if (fieldType == char.class || fieldType == Character.class) {
                    String string = jsonObject.getString(fieldName);
                    if (string.length() != 1) {
                        throw new RuntimeException("Incorrect char format");
                    }
                    field.setChar(prototype, string.charAt(0));
                } else if (fieldType == double.class) {
                    field.setDouble(prototype, jsonObject.getDouble(fieldName));
                } else if (fieldType == float.class) {
                    field.setFloat(prototype, jsonObject.getFloat(fieldName));
                } else if (fieldType == byte.class) {
                    field.setByte(prototype, (byte) jsonObject.getInt(fieldName));
                } else if (fieldType == long.class) {
                    field.setLong(prototype, jsonObject.getLong(fieldName));
                } else {
                    throw new RuntimeException("Unknown primitive");
                }
            } else if (Number.class.isAssignableFrom(fieldType)) {
                if (jsonObject.get(fieldName) == JSONObject.NULL) {
                    field.set(prototype, null);
                } else {
                    if (fieldType == Integer.class) {
                        field.set(prototype, jsonObject.get(fieldName));
                    } else if (fieldType == Boolean.class) {
                        field.set(prototype, jsonObject.getBoolean(fieldName));
                    } else if (fieldType == Short.class) {
                        field.set(prototype, (short) jsonObject.getInt(fieldName));
                    } else if (fieldType == Double.class) {
                        field.set(prototype, jsonObject.getDouble(fieldName));
                    } else if (fieldType == Float.class) {
                        field.set(prototype, jsonObject.getFloat(fieldName));
                    } else if (fieldType == Byte.class) {
                        field.set(prototype, (byte) jsonObject.getInt(fieldName));
                    } else if (fieldType == Long.class) {
                        field.set(prototype, jsonObject.getLong(fieldName));
                    } else {
                        throw new RuntimeException("Unknown primitive");
                    }
                }
            } else {
                field.set(prototype, getField(jsonObject.get(field.getName()), field));
            }
        } catch (IllegalAccessException e) {
            throw new FailedFieldAccessException(e);
        }
    }

    private Object getField(Object object, Field field) throws IllegalAccessException {
        if (object == JSONObject.NULL) {
            return null;
        } else if (Map.class.isAssignableFrom(field.getType())) {
            return getMapField((JSONObject) object, field);
        } else {
            Object result;
            switch (object) {
                case JSONObject jsonObject:
                        result = new JsonDeserializer<>(jsonObject, field.getType()).instance();
                        break;
                case JSONArray objects:
                    Class<?> type = field.getType();
                    if (Collection.class.isAssignableFrom(type)) {
                        result = getCollectionField(objects, field);
                    } else if (type.isArray()) {
                        result = getArray(objects, field.getType().getComponentType());
                    } else {
                        result = null;
                    }
                    break;
                default:
                    result = object;
            }
            return result;
        }
    }

    private Object getCollectionField(JSONArray array, Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length != 1) {
                throw new UnsupportedParametrizedCollection();
            }
            return getCollection(genericType, typeArgs[0], array);
        } else {
            return null;
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
        return new JsonDeserializer<>(arrayElement, (Class<?>) singleType).instance();
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

    private Object getMapField(JSONObject map, Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length != 2) {
                throw new UnsupportedParametrizedMap();
            }
                return getMap(genericType, typeArgs[0], typeArgs[1], map);
        }
        return null;
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
                result = new JsonDeserializer(new JSONObject(value), rawType).instance();
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
                result = new JsonDeserializer(new JSONObject(value), clzz).instance();
            }
        }
        return result;
    }

    @Override
    public Collection<T> collection() {
        Collection<T> list = new ArrayList<>();
        return (Collection<T>) getCollection(Arrays.stream(list.getClass().getGenericInterfaces())
                .filter(x -> x instanceof ParameterizedType parameterizedType
                        && parameterizedType.getRawType() == List.class)
                .findFirst()
                .get(), clazz, (JSONArray) json);
    }

    @Override
    public <K> Map<K, T> map(Class<K> keyClazz) {
        Map<K, T> map = new HashMap<>();
        return (Map<K, T>) getMap(Arrays.stream(map.getClass().getGenericInterfaces())
                .filter(x -> x instanceof ParameterizedType parameterizedType
                        && parameterizedType.getRawType() == Map.class)
                .findAny().get(), keyClazz, clazz, (JSONObject) json);
    }

    private static boolean isEligibleField(Field field) {
        int modifiers = field.getModifiers();
        return !field.isAnnotationPresent(Transient.class)
                && !Modifier.isTransient(modifiers)
                && !Modifier.isFinal(modifiers);
    }
}
