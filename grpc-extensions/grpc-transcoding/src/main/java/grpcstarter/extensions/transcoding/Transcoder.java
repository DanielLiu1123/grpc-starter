package grpcstarter.extensions.transcoding;

import static com.google.protobuf.Descriptors.FieldDescriptor.Type;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.HttpRule;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Freeman
 */
class Transcoder {

    private static JsonFormat.@Nullable Parser parser;

    private final Variable variable;

    private Transcoder(Variable variable) {
        this.variable = variable;
    }

    public static Transcoder create(Variable variable) {
        return new Transcoder(variable);
    }

    public void into(Message.Builder messageBuilder, HttpRule httpRule) throws InvalidProtocolBufferException {

        // The special name `*` can be used in the body mapping to define that
        // every field not bound by the path template should be mapped to the
        // request body.

        if (!httpRule.getBody().isBlank()) {
            var bodyStringOpt =
                    Optional.of(variable.body()).map(e -> new String(e, UTF_8)).filter(e -> !e.isBlank());
            if (bodyStringOpt.isPresent()) {
                if (Objects.equals(httpRule.getBody(), "*")) {
                    merge(messageBuilder, bodyStringOpt.get());
                } else {
                    Descriptors.FieldDescriptor field =
                            messageBuilder.getDescriptorForType().findFieldByName(httpRule.getBody());
                    if (hasBuilder(field)) {
                        Message.Builder fieldBuilder = messageBuilder.getFieldBuilder(field);
                        if (fieldBuilder != null) {
                            merge(fieldBuilder, bodyStringOpt.get());
                        }
                    }
                }
            }
        }

        // Note that when using `*` in the body mapping, it is not possible to
        // have HTTP parameters, as all fields not bound by the path end in
        // the body. This makes this option more rarely used in practice when
        // defining REST APIs. The common usage of `*` is in custom methods
        // which don't use the URL at all for transferring data.

        // Any fields in the request message which are not bound by the path template
        // automatically become HTTP query parameters if there is no HTTP request body.

        // Using parameters when the body is not set.

        Map<String, String[]> parameters = variable.parameters();
        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();

                String[] fieldPath = key.split("\\.");

                // Navigate to the last field descriptor
                Message.Builder lastBuilder = messageBuilder;
                for (int i = 0; i < fieldPath.length - 1; i++) {
                    Descriptors.FieldDescriptor field =
                            lastBuilder.getDescriptorForType().findFieldByName(fieldPath[i]);
                    if (hasBuilder(field)) {
                        lastBuilder = lastBuilder.getFieldBuilder(field);
                    }
                }

                Descriptors.FieldDescriptor field =
                        lastBuilder.getDescriptorForType().findFieldByName(fieldPath[fieldPath.length - 1]);
                if (isValueType(field)) {
                    if (field.isRepeated()) {
                        for (String value : values) {
                            lastBuilder.addRepeatedField(field, parseValue(field, value));
                        }
                    } else {
                        if (values.length > 0 && !lastBuilder.hasField(field) /* not set by request body */) {
                            setValueField(lastBuilder, field, values[0]);
                        }
                    }
                }
            }
        }

        // The path variables **must not** capture the leading "/" character. The reason
        // is that the most common use case "{var}" does not capture the leading "/"
        // character. For consistency, all path variables must share the same behavior.

        // The path variables **must not** refer to any repeated or mapped field,
        // because client libraries are not capable of handling such variable expansion.
        Map<String, String> pathVariables = variable.pathVariables();
        if (pathVariables != null && !pathVariables.isEmpty()) {
            for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Descriptors.FieldDescriptor field =
                        messageBuilder.getDescriptorForType().findFieldByName(key);
                if (isValueType(field)) {
                    setValueField(messageBuilder, field, value);
                }
            }
        }
    }

    public Object out(Message response, HttpRule httpRule) {
        if (!httpRule.getResponseBody().isBlank()) {
            Descriptors.FieldDescriptor field =
                    response.getDescriptorForType().findFieldByName(httpRule.getResponseBody());
            if (field != null) {
                return response.getField(field);
            }
        }
        return response;
    }

    private static void merge(Message.Builder messageBuilder, String bodyString) throws InvalidProtocolBufferException {
        getParser().merge(bodyString, messageBuilder);
    }

    private static boolean hasBuilder(Descriptors.@Nullable FieldDescriptor field) {
        return field != null && !field.isRepeated() && !field.isMapField() && field.getType() == Type.MESSAGE;
    }

    private static void setValueField(
            Message.@Nullable Builder lastBuilder,
            Descriptors.@Nullable FieldDescriptor field,
            @Nullable String values) {
        if (lastBuilder == null || field == null || values == null) return;
        if (isValueType(field)) {
            lastBuilder.setField(field, parseValue(field, values));
        }
    }

    private static boolean isValueType(Descriptors.@Nullable FieldDescriptor field) {
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
                if (Character.isDigit(value.charAt(0))) {
                    try {
                        var e = field.getEnumType().findValueByNumber(Integer.parseInt(value));
                        if (e != null) yield e;
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    var e = field.getEnumType().findValueByName(value);
                    if (e != null) yield e;
                }
                throw new IllegalArgumentException(
                        "Can't parse enum value '" + value + "' for field '" + field.getName() + "'");
            }
            case MESSAGE -> {
                throw new IllegalArgumentException(
                        "Direct parsing to message type not supported, field " + field.getName());
            }
        };
    }

    private static JsonFormat.Parser getParser() {
        if (parser == null) {
            parser = JsonFormat.parser().ignoringUnknownFields();
        }
        return parser;
    }

    public record Variable(
            byte[] body,
            @Nullable Map<String, String[]> parameters,
            @Nullable Map<String, String> pathVariables) {}
}
