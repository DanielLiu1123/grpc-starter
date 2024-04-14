package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static com.google.protobuf.Descriptors.FieldDescriptor.Type;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.HttpRule;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;

/**
 * @author Freeman
 */
public class Transcoder {

    private static final JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();
    private static final JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

    private final Variable variable;

    public Transcoder(Variable variable) {
        this.variable = variable;
    }

    @SneakyThrows
    public void into(@Nonnull Message.Builder messageBuilder, @Nonnull HttpRule httpRule) {
        if (httpRule.getBody().isBlank()) {
            // Note that when using `*` in the body mapping, it is not possible to
            // have HTTP parameters, as all fields not bound by the path end in
            // the body. This makes this option more rarely used in practice when
            // defining REST APIs. The common usage of `*` is in custom methods
            // which don't use the URL at all for transferring data.

            // Any fields in the request message which are not bound by the path template
            // automatically become HTTP query parameters if there is no HTTP request body.

            Optional.ofNullable(variable.parameters()).orElseGet(Map::of).forEach((key, values) -> {
                String[] fieldPath = key.split("\\.");
                Message.Builder currentBuilder = messageBuilder;
                List<Message.Builder> builderPath = new ArrayList<>(); // To store all builders from root to leaf

                // Navigate to the last field descriptor
                for (int i = 0; i < fieldPath.length - 1; i++) {
                    Descriptors.FieldDescriptor field =
                            currentBuilder.getDescriptorForType().findFieldByName(fieldPath[i]);
                    if (field == null || field.getType() != Type.MESSAGE) {
                        // If the field not exists or not a message type, skip the rest of the path
                        return;
                    }
                    currentBuilder = currentBuilder.getFieldBuilder(field);
                    builderPath.add(currentBuilder); // Add builder to path
                }

                Descriptors.FieldDescriptor field =
                        currentBuilder.getDescriptorForType().findFieldByName(fieldPath[fieldPath.length - 1]);
                if (field == null) return;

                // Set the value at the leaf level
                if (field.isRepeated()) {
                    // TODO: repeated message not supported
                    for (String value : values) {
                        currentBuilder.addRepeatedField(field, parseValue(field, value));
                    }
                } else {
                    if (values.length > 0) {
                        currentBuilder.setField(field, parseValue(field, values[0]));
                    }
                }

                // Set all intermediate message builders back up the chain
                for (int i = builderPath.size() - 1; i > 0; i--) {
                    Message.Builder childBuilder = builderPath.get(i);
                    Message.Builder parentBuilder = builderPath.get(i - 1);
                    Descriptors.FieldDescriptor parentField =
                            parentBuilder.getDescriptorForType().findFieldByName(fieldPath[i - 1]);
                    parentBuilder.setField(parentField, childBuilder.build());
                }

                // Finally set the topmost builder back to the original messageBuilder
                if (!builderPath.isEmpty()) {
                    messageBuilder.setField(
                            messageBuilder.getDescriptorForType().findFieldByName(fieldPath[0]),
                            builderPath.get(0).build());
                }
            });
        } else {
            // The special name `*` can be used in the body mapping to define that
            // every field not bound by the path template should be mapped to the
            // request body.
            String bodyString = Optional.ofNullable(variable.body())
                    .map(e -> new String(e, UTF_8))
                    .orElse("");
            if (!bodyString.isBlank()) {
                if (Objects.equals(httpRule.getBody(), "*")) {
                    parser.merge(bodyString, messageBuilder);
                } else {
                    Descriptors.FieldDescriptor field =
                            messageBuilder.getDescriptorForType().findFieldByName(httpRule.getBody());
                    if (field != null && field.getType() == Type.MESSAGE) {
                        Message.Builder fieldBuilder = messageBuilder.getFieldBuilder(field);
                        if (fieldBuilder != null) {
                            parser.merge(bodyString, fieldBuilder);
                            messageBuilder.setField(field, fieldBuilder.build());
                        }
                    }
                }
            }
        }

        // The path variables **must not** capture the leading "/" character. The reason
        // is that the most common use case "{var}" does not capture the leading "/"
        // character. For consistency, all path variables must share the same behavior.
        Optional.ofNullable(variable.pathVariables()).orElseGet(Map::of).forEach((key, value) -> {
            Descriptors.FieldDescriptor field =
                    messageBuilder.getDescriptorForType().findFieldByName(key);
            if (field == null) return;

            // The path variables **must not** refer to any repeated or mapped field,
            // because client libraries are not capable of handling such variable expansion.
            if (field.isRepeated() || field.isMapField()) return;

            messageBuilder.setField(field, parseValue(field, value));
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

    private void setField(Message.Builder builder, Descriptors.FieldDescriptor field, String... values) {
        if (field == null || field.isMapField()) return;

        if (field.isRepeated()) {
            for (String value : values) {
                builder.addRepeatedField(field, parseValue(field, value));
            }
        } else {
            if (values.length > 0) {
                builder.setField(field, parseValue(field, values[0]));
            }
        }
    }

    private Message.Builder getNestedBuilder(Message.Builder builder, String[] parts, int start, int end) {
        Message.Builder currentBuilder = builder;
        for (int i = start; i <= end; i++) {
            Descriptors.FieldDescriptor field =
                    currentBuilder.getDescriptorForType().findFieldByName(parts[i]);
            if (field != null && field.getType() == Type.MESSAGE) {
                currentBuilder = currentBuilder.getFieldBuilder(field);
            }
        }
        return currentBuilder;
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
                Descriptors.EnumValueDescriptor enumValueDescriptor =
                        field.getEnumType().findValueByName(value);
                if (enumValueDescriptor != null) {
                    yield enumValueDescriptor;
                }
                throw new IllegalArgumentException(
                        "Enum value " + value + " not recognized for field " + field.getName());
            }
            case MESSAGE -> throw new IllegalArgumentException(
                    "Direct parsing to message type not supported, field " + field.getName());
        };
    }

    public record Variable(byte[] body, Map<String, String[]> parameters, Map<String, String> pathVariables) {}
}
