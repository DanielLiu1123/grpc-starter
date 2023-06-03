package com.freemanan.starter.grpc.extensions.jsontranscoder.util;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
public class ProtoUtil {

    private static final JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

    /**
     * Check if protobuf message is simple value.
     *
     * @param message protobuf message
     * @return true if message is simple value
     */
    public static boolean isSimpleValueMessage(Message message) {
        if (isWrapperType(message.getClass())) {
            return true;
        }
        if (message instanceof Value) {
            Value value = (Value) message;
            Value.KindCase kind = value.getKindCase();
            return kind == Value.KindCase.NULL_VALUE
                    || kind == Value.KindCase.NUMBER_VALUE
                    || kind == Value.KindCase.STRING_VALUE
                    || kind == Value.KindCase.BOOL_VALUE;
        }
        return false;
    }

    public static boolean isWrapperType(Class<?> clz) {
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

    /**
     * Convert protobuf message to JSON string.
     *
     * <p> Wrapper types (Int32Value, Int64Value, etc. ) will be converted to simple value.
     * <p> If kind of {@link Value} is {@link Value.KindCase#NULL_VALUE}, {@link Value.KindCase#BOOL_VALUE}, {@link Value.KindCase#NUMBER_VALUE} or {@link Value.KindCase#STRING_VALUE}, it will be converted to simple value.
     *
     * @param message protobuf message
     * @return JSON string
     */
    public static String toJson(Message message) {
        try {
            return printer.print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Can't convert message to JSON", e);
        }
    }
}
