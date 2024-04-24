package grpcstarter.extensions.transcoding;

import static com.google.protobuf.Descriptors.FieldDescriptor.Type;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.HttpRule;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Freeman
 */
class Transcoder {

    private static final JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();

    private final Variable variable;

    private Transcoder(Variable variable) {
        this.variable = variable;
    }

    public static Transcoder create(Variable variable) {
        return new Transcoder(variable);
    }

    public void into(@Nonnull Message.Builder messageBuilder, @Nonnull HttpRule httpRule) {
        // Note that when using `*` in the body mapping, it is not possible to
        // have HTTP parameters, as all fields not bound by the path end in
        // the body. This makes this option more rarely used in practice when
        // defining REST APIs. The common usage of `*` is in custom methods
        // which don't use the URL at all for transferring data.

        // Any fields in the request message which are not bound by the path template
        // automatically become HTTP query parameters if there is no HTTP request body.

        Optional.ofNullable(variable.parameters()).orElseGet(Map::of).forEach((key, values) -> {
            String[] fieldPath = key.split("\\.");

            // Navigate to the last field descriptor
            Message.Builder lastBuilder = messageBuilder;
            for (int i = 0; i < fieldPath.length - 1; i++) {
                Descriptors.FieldDescriptor field =
                        lastBuilder.getDescriptorForType().findFieldByName(fieldPath[i]);
                if (noBuilder(field)) return;

                lastBuilder = lastBuilder.getFieldBuilder(field);
            }

            Descriptors.FieldDescriptor field =
                    lastBuilder.getDescriptorForType().findFieldByName(fieldPath[fieldPath.length - 1]);
            if (!isValueType(field)) return;

            if (field.isRepeated()) {
                for (String value : values) {
                    lastBuilder.addRepeatedField(field, parseValue(field, value));
                }
            } else {
                if (values.length > 0) {
                    setValueField(lastBuilder, field, values[0]);
                }
            }
        });

        // The special name `*` can be used in the body mapping to define that
        // every field not bound by the path template should be mapped to the
        // request body.

        if (!httpRule.getBody().isBlank()) {
            String bodyString = Optional.ofNullable(variable.body())
                    .map(e -> new String(e, UTF_8))
                    .orElse("");
            if (!bodyString.isBlank()) {
                if (Objects.equals(httpRule.getBody(), "*")) {
                    merge(messageBuilder, bodyString);
                } else {
                    Descriptors.FieldDescriptor field =
                            messageBuilder.getDescriptorForType().findFieldByName(httpRule.getBody());
                    if (noBuilder(field)) return;

                    Message.Builder fieldBuilder = messageBuilder.getFieldBuilder(field);
                    if (fieldBuilder != null) {
                        merge(fieldBuilder, bodyString);
                    }
                }
            }
        }

        // The path variables **must not** capture the leading "/" character. The reason
        // is that the most common use case "{var}" does not capture the leading "/"
        // character. For consistency, all path variables must share the same behavior.

        // The path variables **must not** refer to any repeated or mapped field,
        // because client libraries are not capable of handling such variable expansion.
        Optional.ofNullable(variable.pathVariables()).orElseGet(Map::of).forEach((key, value) -> {
            Descriptors.FieldDescriptor field =
                    messageBuilder.getDescriptorForType().findFieldByName(key);
            if (!isValueType(field)) return;
            setValueField(messageBuilder, field, value);
        });
    }

    public Object out(@Nonnull Message response, @Nonnull HttpRule httpRule) {
        if (!httpRule.getResponseBody().isBlank()) {
            Descriptors.FieldDescriptor field =
                    response.getDescriptorForType().findFieldByName(httpRule.getResponseBody());
            if (field != null) {
                return response.getField(field);
            }
        }
        return response;
    }

    private static void merge(Message.Builder messageBuilder, String bodyString) {
        try {
            parser.merge(bodyString, messageBuilder);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Merge JSON to message failed", e);
        }
    }

    private static boolean noBuilder(Descriptors.FieldDescriptor field) {
        return field == null || field.isRepeated() || field.isMapField() || field.getType() != Type.MESSAGE;
    }

    private static void setValueField(Message.Builder lastBuilder, Descriptors.FieldDescriptor field, String values) {
        if (lastBuilder == null || field == null || values == null) return;
        if (isValueType(field)) {
            lastBuilder.setField(field, parseValue(field, values));
        }
    }

    private static boolean isValueType(Descriptors.FieldDescriptor field) {
        return field != null
                && switch (field.getJavaType()) {
                    case INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING, BYTE_STRING, ENUM -> true;
                    default -> false;
                };
    }

    private static Object parseValue(Descriptors.FieldDescriptor field, String value) {
        return switch (field.getJavaType()) {
            case INT -> Integer.parseInt(value);
            case LONG -> Long.parseLong(value);
            case FLOAT -> Float.parseFloat(value);
            case DOUBLE -> Double.parseDouble(value);
            case BOOLEAN -> Boolean.parseBoolean(value);
            case STRING -> value;
            case BYTE_STRING -> ByteString.copyFrom(value.getBytes(UTF_8));
            case ENUM -> {
                if (value.isBlank()) {
                    yield field.getEnumType().getValues().get(0);
                }
                Descriptors.EnumValueDescriptor e = null;
                if (Character.isDigit(value.charAt(0))) {
                    try {
                        e = field.getEnumType().findValueByNumber(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    e = field.getEnumType().findValueByName(value);
                }
                if (e != null) {
                    yield e;
                }
                throw new IllegalArgumentException(
                        "Can't parse enum value '" + value + "' for field '" + field.getName() + "'");
            }
            case MESSAGE -> throw new IllegalArgumentException(
                    "Direct parsing to message type not supported, field " + field.getName());
        };
    }

    public record Variable(byte[] body, Map<String, String[]> parameters, Map<String, String> pathVariables) {}
}
