package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.Util.isSimpleValueMessage;
import static grpcstarter.extensions.transcoding.Util.stringifySimpleValueMessage;

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
    private static JsonFormat.Printer printer;

    static {
        om = new Jackson2ObjectMapperBuilder()
                .failOnEmptyBeans(false)
                .modules(new SimpleModule().addSerializer(new ProtoMessageSerializer()))
                .build();
    }

    /**
     * Convert the Java bean or Protobuf {@link Message} to JSON string.
     *
     * <p> For Java Bean: include all fields, even if they are null.
     * <p> For Protobuf {@link Message}: use {@link #printer} to print.
     *
     * @param obj the object/{@link Message} to encode
     * @return json string
     */
    public static String toJson(Object obj) {
        if (obj instanceof Message m) {
            if (isSimpleValueMessage(m)) {
                return stringifySimpleValueMessage(m);
            }
            try {
                return getPrinter().print(m);
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

    public static boolean canParseJson(Object obj) {
        if (obj instanceof Message m) {
            return !isSimpleValueMessage(m);
        }
        return !BeanUtils.isSimpleValueType(obj.getClass());
    }

    public static void setPrinter(JsonFormat.Printer printer) {
        JsonUtil.printer = printer;
    }

    private static JsonFormat.Printer getPrinter() {
        if (printer == null) {
            printer = JsonFormat.printer().omittingInsignificantWhitespace();
        }
        return printer;
    }

    private static final class ProtoMessageSerializer extends StdSerializer<MessageOrBuilder> {

        private ProtoMessageSerializer() {
            super(MessageOrBuilder.class);
        }

        @Override
        public void serialize(MessageOrBuilder value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeRawValue(getPrinter().print(value));
        }
    }
}
