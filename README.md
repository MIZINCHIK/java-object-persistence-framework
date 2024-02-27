**Object Persistence in Java as a part of the Software Design cours at NSU**

_Contributions by Maxim Chernyshov & Artyom Blaginin_

**Serialization requirements**:

1. Objects are serialized in JSON, either in String or directly to files.
2. Serialization is supported for the following types: primitives, boxed types, Collections, Maps, Arrays (last three work with all other types) and composite DTOs/records with any other supported type in its fields.
3. Fields with the transient modifier aren’t serialized.
4. Fields with a custom @Transient annotation aren’t serialized.
5. [Preliminary] Objects with cyclic dependencies are dealt in a special way: after the first mention an object that’s appeared previously is to be mentioned in a capturing group-like form (e.g. %obj:xxx%, where xxx is the index of the capturing group).

**Deserialization requirements**:
1. Objects are deserialized from the same format serializator utilizes.
2. In order to deserialize an object the user not only provides the source but also specifies the Class of the mentioned object.
3. Transient and @Transient fields are initialized with default or null values.

**Filtering requirements**:
1. Filtering works on a custom stream-like object initialized via a special method like new PersistentStream(String json, Class<?> clazz).
2. Logic filters are organized just like filters in Java Stream API.
3. Filtering and streaming is performed on serialized data.
