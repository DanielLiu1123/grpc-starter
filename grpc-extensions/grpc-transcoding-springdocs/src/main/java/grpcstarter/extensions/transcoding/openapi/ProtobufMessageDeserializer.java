package grpcstarter.extensions.transcoding.openapi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.springframework.util.ReflectionUtils;

/**
 * Protobuf message deserializer, use {@link JsonFormat#parser()} to deserialize protobuf message.
 *
 * @param <T> protobuf message type
 */
final class ProtobufMessageDeserializer<T extends MessageOrBuilder> extends JsonDeserializer<T> {

    private static final JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();

    private final Class<T> clazz;

    public ProtobufMessageDeserializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        var treeNode = p.readValueAsTree();

        String json = treeNode.toString();

        var newBuilderMethod = ReflectionUtils.findMethod(clazz, "newBuilder");
        if (newBuilderMethod == null) {
            throw new IllegalStateException("No newBuilder method found for class " + clazz);
        }

        try {
            var builder = (Message.Builder) newBuilderMethod.invoke(null);

            parser.merge(json, builder);

            @SuppressWarnings("unchecked")
            T result = (T) builder.build();

            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to deserialize protobuf message", e);
        }
    }
}
