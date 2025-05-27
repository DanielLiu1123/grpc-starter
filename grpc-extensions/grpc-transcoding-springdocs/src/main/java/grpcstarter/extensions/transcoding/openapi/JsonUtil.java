package grpcstarter.extensions.transcoding.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * JSON utility class for serializing Java objects to JSON and deserializing JSON to Java objects.
 *
 * <p>This utility provides a consistent way to handle JSON serialization and deserialization
 * across the application. It uses Jackson's ObjectMapper to support Java objects.</p>
 *
 * @author Freeman
 */
final class JsonUtil {

    private JsonUtil() {
        throw new IllegalStateException("Utility class cannot be instantiated!");
    }

    private static final ObjectMapper objectMapper = buildObjectMapper();

    /**
     * Convert the object to JSON string.
     *
     * <p>Examples:</p>
     *
     * <pre>{@code
     * // Serialize a simple Java object
     * User user = new User().setName("Freeman").setAge(18);
     * String json = JsonUtil.toJson(user);
     * // Result: {"name":"Freeman","age":18}
     *
     * // Serialize a Map
     * Map<String, Object> map = Map.of("name", "Freeman", "age", 18);
     * String json = JsonUtil.toJson(map);
     * // Result: {"name":"Freeman","age":18}
     *
     * // Serialize a List
     * List<String> list = List.of("apple", "banana", "orange");
     * String json = JsonUtil.toJson(list);
     * // Result: ["apple","banana","orange"]
     * }</pre>
     *
     * @param obj the object to encode
     * @return json string
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Convert the json string to object.
     *
     * <p>Examples:</p>
     *
     * <pre>{@code
     * // Deserialize JSON to a Java object
     * String json = "{\"name\":\"Freeman\",\"age\":18}";
     * User user = JsonUtil.toBean(json, User.class);
     * // Result: User(name=Freeman, age=18)
     * }</pre>
     *
     * @param json  the json string to decode
     * @param clazz the class of the object
     * @param <T>   the type of the object
     * @return the decoded object
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Convert the json string to a specific bean list.
     *
     * <p>This method is specifically designed for deserializing JSON arrays into lists of objects.</p>
     *
     * <p>Examples:</p>
     *
     * <pre>{@code
     * // Deserialize JSON array to a List of User objects
     * String json = "[{\"name\":\"Bob\",\"age\":18},{\"name\":\"Jason\",\"age\":18}]";
     * List<User> users = JsonUtil.toList(json, User.class);
     * // Result: List containing User(name=Bob, age=18) and User(name=Jason, age=18)
     * }</pre>
     *
     * @param json  json string
     * @param clazz class
     * @param <T>   the type of the object
     * @return list of objects of type T
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(
                    json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Convert the JSON string to object.
     *
     * <p>This method is particularly useful for deserializing JSON into complex generic types
     * or when the exact type is not known at compile time.</p>
     *
     * <p>Examples:</p>
     *
     * <pre>{@code
     * // Deserialize JSON to a Map
     * String json = """
     *      {"name": "fm", "age": 18, "hobbies": ["reading", "coding"]}
     *    """;
     * Map<String, Object> map = JsonUtil.toBean(json, new ParameterizedTypeReference<>() {});
     * // Result: {name=fm, age=18, hobbies=[reading, coding]}
     * }</pre>
     *
     * <pre>{@code
     * // Deserialize JSON array to a List
     * String json = """
     *      [1, 2, 3]
     *    """;
     * List<Integer> list = JsonUtil.toBean(json, new ParameterizedTypeReference<>() {});
     * // Result: [1, 2, 3]
     * }</pre>
     *
     * <pre>{@code
     * // Deserialize JSON to a complex generic type
     * String json = """
     *      {"data": [{"id": 1, "name": "item1"}, {"id": 2, "name": "item2"}], "total": 2}
     *    """;
     * PageResult<Item> result = JsonUtil.toBean(json, new ParameterizedTypeReference<>() {});
     * // Result: PageResult with data=[Item(id=1, name=item1), Item(id=2, name=item2)] and total=2
     * }</pre>
     *
     * @param json    json string
     * @param typeRef type reference
     * @param <T>     the type of the object
     * @return java bean
     */
    public static <T> T toBean(String json, ParameterizedTypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
                @Override
                public Type getType() {
                    return typeRef.getType();
                }
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static ObjectMapper buildObjectMapper() {
        var objectMapper = new ObjectMapper();

        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        registerWellKnownModulesIfAvailable(objectMapper);

        // 防止 Long 精度丢失
        objectMapper.registerModule(new SimpleModule() {
            {
                addSerializer(Long.class, ToStringSerializer.instance);
                addSerializer(Long.TYPE, ToStringSerializer.instance);
                addSerializer(BigInteger.class, ToStringSerializer.instance);
            }
        });

        if (ClassUtils.isPresent("com.google.protobuf.Message", null)) {
            objectMapper.registerModule(new ProtobufModule());
        }

        return objectMapper;
    }

    /**
     * copy from {@link Jackson2ObjectMapperBuilder#registerWellKnownModulesIfAvailable(MultiValueMap)}
     */
    @SuppressWarnings("unchecked")
    private static void registerWellKnownModulesIfAvailable(ObjectMapper objectMapper) {
        try {
            Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>)
                    ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", null);
            Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
            objectMapper.registerModule(jdk8Module);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jdk8 not available
        }

        try {
            Class<? extends Module> parameterNamesModuleClass = (Class<? extends Module>)
                    ClassUtils.forName("com.fasterxml.jackson.module.paramnames.ParameterNamesModule", null);
            Module parameterNamesModule = BeanUtils.instantiateClass(parameterNamesModuleClass);
            objectMapper.registerModule(parameterNamesModule);
        } catch (ClassNotFoundException ex) {
            // jackson-module-parameter-names not available
        }

        try {
            Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>)
                    ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", null);
            Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
            objectMapper.registerModule(javaTimeModule);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jsr310 not available
        }

        // Kotlin present?
        if (KotlinDetector.isKotlinPresent()) {
            try {
                Class<? extends Module> kotlinModuleClass = (Class<? extends Module>)
                        ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", null);
                Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
                objectMapper.registerModule(kotlinModule);
            } catch (ClassNotFoundException ex) {
                // jackson-module-kotlin not available
            }
        }
    }
}
