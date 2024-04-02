# JVIVO
## JSON-based object persistence framework
### _Contributions by Maxim Chernyshov & Artyom Blaginin as part of the Software Design course at NSU_
#### Powered by JSON-java (org.json)

## **API overview**:
### Serializing
1) Base interface -- Serializer, its bas implementation -- JsonSerializer
2) Objects are serialized in JSON format either into a String or directly into a file
3) Serialization is supported for the following types: primitives, boxed types, Collections, Maps, Arrays (last three work with all other types) and composite DTOs/records with any other supported type in its fields
4) Serialization of interfaces and generic types other than Collections, Maps and Arrays and of types with interface fields other than those metioned before is irreversible
5) Fields with either the transient modifier or a @Transient annotation aren’t serialized
```java
public interface Serializer {
    String serialize(Object object);

    void serialize(Object object, File file);
}
```

### Deserializing
1) Base interface -- Deserializer\<T\>, its base implementation -- JsonDeserializer\<T\>
2) Objects are deserialized from JSON format into an instance, collection or a map
3) Only objects the serialization of which is reversible may be deserialized
4) In order to deserialize an object the user not only provides the source but also specifies the Class of the mentioned object
5) Fields modified with transient and annotated with @Transient are initialized with default or null values
```java
public interface Deserializer<T> {
    T instance();

    Collection<T> collection();

    <K> Map<K, T> map(Class<K> keyClazz);
}
```

### Filtering
1) Base class -- AttributeFilter
2) Filtering is left-associative but grouping operations into a single AttributeFilter is akin to using braces
3) Filter holds information whether it's evaluation result to be applied with con- or disjunction
4) Filters are constructed with a base Predicate and then and(), or() and not() operations stack filters up
```java
public class AttributeFilter {
    public AttributeFilter(String attributeName, Predicate<?> predicate) //...
    // ...
    public Set<String> getNecessaryFields() //...
    public AttributeFilter not() //...
    public void setOr() //...
    public void setAnd() //...
    public void reverseAnd() //...
    public AttributeFilter and(AttributeFilter filter) //...
    public AttributeFilter or(AttributeFilter filter) //...
    public boolean evaluate(Map<String, Object> necessaryObjects) //...
```

### Streaming
1) Base interface -- SerialStream\<T\>, its base implementation -- JsonSerialStream\<T\>
2) Streaming is required for maintaining a session (see below) but may be used independently as all the other components
3) Stream takes JSON strings and files, applies filters and outputs a list of the given type
4) Invalid JSONs are silently skipped
```java
public interface SerialStream<T> {
    SerialStream<T> add(String json);

    SerialStream<T> addAll(Collection<String> jsons);

    SerialStream<T> add(File json);

    SerialStream<T> addAllFiles(Collection<File> jsons);

    List<T> toList();

    List<T> toList(AttributeFilter filter);

    List<T> toListExclude(AttributeFilter filter);
}
```

### Session
1) Base interface -- Session, its base implementation -- JsonSession
2) Session automatically controls the entities: deletes, finds and stores them on a disk
3) Session may be used to acquire a serializer or deserializer and avoid using those components directly
4) Only after persist call the changes are applied to the disk
5) The exception to 4) is finding with a filter. Before the filter is applied all the changes are persisted
6) In the specified directory session creates a subdirectory ./jvivo and when necessary it adds directories for each unique class and fills them with *.jvivo files storing JSONs
7) Session doesn't support the types that are irreversibly serialized
```java
public interface Session {
    void setDirectory(Path directory);

    <T> void insert(T object);

    <T> List<T> find(Class<T> clazz);

    <T> List<T> find(Class<T> clazz, AttributeFilter filter);

    <T> void delete(Class<T> clazz, AttributeFilter filter);

    void persist();

    <T> Deserializer<T> getDeserializer(Object jsonObject, Class<T> clazz);

    <T> Deserializer<T> getDeserializer(JSONObject jsonObject, Class<T> clazz);

    <T> SerialStream<T> getSerializer(Class<T> clazz);
}
```

#### For further information and more examples see the tests