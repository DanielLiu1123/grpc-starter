package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.Util.isSimpleValueMessage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Utility class for JSON serialization, aimed to support both Java bean and Protobuf {@link Message}.
 *
 * @author Freeman
 */
final class JsonUtil {

    private JsonUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    private static final JsonMapper om;

    private static JsonFormat.@Nullable Printer printer;

    static {
        om = JsonMapper.builder()
                .addModules(new SimpleModule().addSerializer(new ProtoMessageSerializer()))
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
        return om.writeValueAsString(obj);
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
        public void serialize(MessageOrBuilder value, JsonGenerator gen, SerializationContext provider)
                throws JacksonException {
            try {
                gen.writeRawValue(getPrinter().print(value));
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalStateException("Print failed", e);
            }
        }
    }
}
