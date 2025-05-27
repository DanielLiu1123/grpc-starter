package grpcstarter.extensions.transcoding.openapi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.ProtocolMessageEnum;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Protobuf enum deserializer, unrecognized number will be deserialized as UNRECOGNIZED.
 *
 * @param <T> protobuf enum type
 */
final class ProtobufEnumDeserializer<T extends Enum<T> & ProtocolMessageEnum> extends JsonDeserializer<T> {

    private final Class<T> clazz;

    public ProtobufEnumDeserializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        var treeNode = p.readValueAsTree();

        if (treeNode.isValueNode()) {
            if (treeNode instanceof NumericNode numericNode) {
                return convertNumberToEnum(numericNode.intValue());
            }
            if (treeNode instanceof TextNode textNode) {
                return convertStringToEnum(textNode.asText());
            }
        }

        throw new IllegalArgumentException(
                "Can't deserialize protobuf enum '" + clazz.getSimpleName() + "' from " + treeNode);
    }

    private T convertStringToEnum(String text) {
        var enums = clazz.getEnumConstants();
        if (enums == null) {
            throw new IllegalStateException("No enum constants found for class " + clazz);
        }

        for (var e : enums) {
            if (Objects.equals(e.name(), text)) {
                return e;
            }
        }

        return getUnrecognizedEnum(enums);
    }

    private T convertNumberToEnum(int number) {
        var enums = clazz.getEnumConstants();
        if (enums == null) {
            throw new IllegalStateException("No enum constants found for class " + clazz);
        }

        for (var e : enums) {
            if (!Objects.equals(e.name(), "UNRECOGNIZED")) { // UNRECOGNIZED getNumber() will throw exception
                if (e.getNumber() == number) {
                    return e;
                }
            }
        }

        // return UNRECOGNIZED
        return getUnrecognizedEnum(enums);
    }

    private T getUnrecognizedEnum(T[] enums) {
        return Arrays.stream(enums)
                .filter(e -> Objects.equals(e.name(), "UNRECOGNIZED"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No UNRECOGNIZED enum constant found for class " + clazz));
    }
}
