package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.Util.isSimpleValueMessage;
import static grpcstarter.extensions.transcoding.Util.snakeToPascal;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UtilTest {
    /**
     * {@link Util#isSimpleValueMessage(Message)}
     */
    @Test
    void testSimpleValue() {
        assertThat(isSimpleValueMessage(StringValue.of(""))).isTrue();
        assertThat(isSimpleValueMessage(Int32Value.of(1))).isTrue();
        assertThat(isSimpleValueMessage(UInt32Value.of(1))).isTrue();
        assertThat(isSimpleValueMessage(Int64Value.of(1))).isTrue();
        assertThat(isSimpleValueMessage(UInt64Value.of(1))).isTrue();
        assertThat(isSimpleValueMessage(BoolValue.of(true))).isTrue();
        assertThat(isSimpleValueMessage(FloatValue.of(1))).isTrue();
        assertThat(isSimpleValueMessage(DoubleValue.of(1))).isTrue();
        assertThat(isSimpleValueMessage(BytesValue.of(ByteString.copyFrom("Freeman", StandardCharsets.UTF_8))))
                .isTrue();

        assertThat(isSimpleValueMessage(Value.newBuilder().setBoolValue(true).build()))
                .isTrue();
        assertThat(isSimpleValueMessage(
                        Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build()))
                .isTrue();
        assertThat(isSimpleValueMessage(Value.newBuilder().setNumberValue(1).build()))
                .isTrue();
        assertThat(isSimpleValueMessage(Value.newBuilder().setStringValue("1").build()))
                .isTrue();
        assertThat(isSimpleValueMessage(Value.newBuilder()
                        .setStructValue(Struct.newBuilder().build())
                        .build()))
                .isFalse();
        assertThat(isSimpleValueMessage(Value.newBuilder()
                        .setListValue(ListValue.newBuilder().build())
                        .build()))
                .isFalse();
        assertThat(isSimpleValueMessage(Struct.newBuilder().build())).isFalse();
        assertThat(isSimpleValueMessage(ListValue.newBuilder().build())).isFalse();
    }

    @Test
    void testJsonValue() throws InvalidProtocolBufferException {
        JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
        Value v = Value.newBuilder()
                .setStructValue(Struct.newBuilder()
                        .putFields(
                                "a",
                                Value.newBuilder()
                                        .setNullValue(NullValue.NULL_VALUE)
                                        .build())
                        .putFields("b", Value.newBuilder().setNumberValue(1).build())
                        .putFields("c", Value.newBuilder().setStringValue("2").build())
                        .putFields("d", Value.newBuilder().setBoolValue(true).build())
                        .putFields(
                                "e",
                                Value.newBuilder()
                                        .setListValue(ListValue.newBuilder()
                                                .addValues(Value.newBuilder()
                                                        .setNullValue(NullValue.NULL_VALUE)
                                                        .build())
                                                .addValues(Value.newBuilder()
                                                        .setNumberValue(1)
                                                        .build())
                                                .addValues(Value.newBuilder()
                                                        .setStringValue("2")
                                                        .build())
                                                .addValues(Value.newBuilder()
                                                        .setBoolValue(true)
                                                        .build())
                                                .build())
                                        .build())
                        .build())
                .build();
        assertThat(printer.print(v))
                .isEqualTo("{\"a\":null,\"b\":1.0,\"c\":\"2\",\"d\":true,\"e\":[null,1.0,\"2\",true]}");
    }

    @Test
    void testSimpleValueParse2Message() throws InvalidProtocolBufferException {
        JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();
        JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

        String json = "Freeman";

        StringValue.Builder builder = StringValue.newBuilder();
        parser.merge(json, builder);

        assertThat(printer.print(builder.build())).isEqualTo("\"Freeman\"");

        json = "1";
        Value.Builder valueBuilder = Value.newBuilder();
        parser.merge(json, valueBuilder);

        assertThat(printer.print(valueBuilder.build())).isEqualTo("1.0");
    }

    /**
     * {@link Util#snakeToPascal(String)}
     */
    @Test
    void testSnakeToPascal() {
        assertThat(snakeToPascal("snake_case")).isEqualTo("SnakeCase");
        assertThat(snakeToPascal("s_s")).isEqualTo("SS");
        assertThat(snakeToPascal("s")).isEqualTo("S");
        assertThat(snakeToPascal("Ss")).isEqualTo("Ss");
        assertThat(snakeToPascal("SS")).isEqualTo("SS");
        assertThat(snakeToPascal("Ss_ss")).isEqualTo("SsSs");
        assertThat(snakeToPascal("SsSs")).isEqualTo("SsSs");
        assertThat(snakeToPascal("ssSs")).isEqualTo("SsSs");
    }
}
