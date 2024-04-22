package com.freemanan.starter.grpc.extensions.jsontranscoder;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
public class ProtoUtil {

    private static final JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

    /**
     * Check if the protobuf message is a simple value.
     *
     * @param message protobuf message
     * @return true if the message is simple value
     */
    public static boolean isSimpleValueMessage(Message message) {
        if (isWrapperType(message.getClass())) {
            return true;
        }
        if (message instanceof Value value) {
            Value.KindCase kind = value.getKindCase();
            return kind == Value.KindCase.NULL_VALUE
                    || kind == Value.KindCase.NUMBER_VALUE
                    || kind == Value.KindCase.STRING_VALUE
                    || kind == Value.KindCase.BOOL_VALUE;
        }
        return false;
    }

    public static Res stringfy(Message message) {
        if (message instanceof BoolValue boolValue) {
            return new Res(true, String.valueOf(boolValue.getValue()));
        }
        if (message instanceof Int32Value int32Value) {
            return new Res(true, String.valueOf(int32Value.getValue()));
        }
        if (message instanceof Int64Value int64Value) {
            return new Res(true, String.valueOf(int64Value.getValue()));
        }
        if (message instanceof UInt32Value uInt32Value) {
            return new Res(true, String.valueOf(uInt32Value.getValue()));
        }
        if (message instanceof UInt64Value uInt64Value) {
            return new Res(true, String.valueOf(uInt64Value.getValue()));
        }
        if (message instanceof FloatValue floatValue) {
            return new Res(true, String.valueOf(floatValue.getValue()));
        }
        if (message instanceof DoubleValue doubleValue) {
            return new Res(true, String.valueOf(doubleValue.getValue()));
        }
        if (message instanceof StringValue stringValue) {
            return new Res(true, stringValue.getValue());
        }
        if (message instanceof BytesValue bytesValue) {
            return new Res(true, bytesValue.getValue().toStringUtf8());
        }
        if (message instanceof Value value) {
            Value.KindCase kind = value.getKindCase();
            if (kind == Value.KindCase.NULL_VALUE) {
                return new Res(true, "null");
            }
            if (kind == Value.KindCase.NUMBER_VALUE) {
                return new Res(true, String.valueOf(value.getNumberValue()));
            }
            if (kind == Value.KindCase.STRING_VALUE) {
                return new Res(true, value.getStringValue());
            }
            if (kind == Value.KindCase.BOOL_VALUE) {
                return new Res(true, String.valueOf(value.getBoolValue()));
            }
        }
        return new Res(false, null);
    }

    /**
     * Convert a protobuf message to JSON string.
     *
     * <p> Wrapper types (Int32Value, Int64Value, etc.) will be converted to simple value.
     * <p> If kind of {@link Value} is {@link Value.KindCase#NULL_VALUE}, {@link Value.KindCase#BOOL_VALUE}, {@link Value.KindCase#NUMBER_VALUE} or {@link Value.KindCase#STRING_VALUE}, it will be converted to simple value.
     *
     * @param message protobuf message
     * @return JSON string
     */
    @SneakyThrows
    public static String toJson(Message message) {
        return printer.print(message);
    }

    private static boolean isWrapperType(Class<?> clz) {
        return BoolValue.class.isAssignableFrom(clz)
                || Int32Value.class.isAssignableFrom(clz)
                || Int64Value.class.isAssignableFrom(clz)
                || UInt32Value.class.isAssignableFrom(clz)
                || UInt64Value.class.isAssignableFrom(clz)
                || FloatValue.class.isAssignableFrom(clz)
                || DoubleValue.class.isAssignableFrom(clz)
                || StringValue.class.isAssignableFrom(clz)
                || BytesValue.class.isAssignableFrom(clz);
    }

    public record Res(boolean isValueMessage, String stringValue) {}
}
