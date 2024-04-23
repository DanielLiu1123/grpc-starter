package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.stringify;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Utility class for JSON serialization, aimed to support both Java bean and Protobuf {@link Message}.
 *
 * @author Freeman
 */
@UtilityClass
class JsonUtil {

    private static final ObjectMapper om;
    private static final JsonFormat.Printer printer;

    static {
        // Use Jackson2ObjectMapperBuilder to be consistent with Spring Boot behavior
        om = new Jackson2ObjectMapperBuilder()
                .failOnEmptyBeans(false)
                .modules(new SimpleModule().addSerializer(new ProtoMessageSerializer()))
                .build();
        printer = JsonFormat.printer().omittingInsignificantWhitespace();
    }

    /**
     * Convert the Java bean or Protobuf {@link Message} to JSON string.
     *
     * <p> For Java Bean: include all fields, even if they are null.
     * <p> For Protobuf {@link Message}: use {@link JsonFormat.Printer#print(MessageOrBuilder)}'s default behavior, default value will be omitted.
     *
     * @param obj the object/{@link Message} to encode
     * @return json string
     */
    public static String toJson(Object obj) {
        if (obj instanceof Message m) {
            var res = stringify(m);
            if (res.isValueMessage()) {
                return res.stringValue();
            }
            try {
                return printer.print(m);
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (BeanUtils.isSimpleValueType(obj.getClass())) {
            return String.valueOf(obj);
        }

        try {
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final class ProtoMessageSerializer extends StdSerializer<MessageOrBuilder> {

        private ProtoMessageSerializer() {
            super(MessageOrBuilder.class);
        }

        @Override
        public void serialize(MessageOrBuilder value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeRawValue(printer.print(value));
        }
    }
}
