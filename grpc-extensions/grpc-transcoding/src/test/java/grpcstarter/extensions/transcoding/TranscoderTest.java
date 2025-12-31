package grpcstarter.extensions.transcoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.api.HttpRule;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * {@link Transcoder} tester.
 */
class TranscoderTest {

    @Test
    void testInto() {
        var request = buildRequest(
                Map.of("requestMessage", "y3"),
                """
                {"some_message": "x1", "requestMessage": "y1"}
                """,
                Map.of(
                        "some_message", new String[] {"x2"},
                        "requestMessage", new String[] {"y2"}));

        assertThat(request.getSomeMessage()).isEqualTo("x2"); // body is ignored when httpRule body is empty
        assertThat(request.getRequestMessage()).isEqualTo("y3");
        assertThat(request.hasNested()).isFalse();

        request = buildRequest(
                Map.of("requestMessage", "y3"),
                """
                {"some_message": "x1", "requestMessage": "y1"}
                """,
                Map.of(
                        "some_message", new String[] {"x2"},
                        "requestMessage", new String[] {"y2"}),
                HttpRule.newBuilder().setBody("*").build());

        assertThat(request.getSomeMessage()).isEqualTo("x1"); // body is used when httpRule body is *
        assertThat(request.getRequestMessage()).isEqualTo("y3");
        assertThat(request.hasNested()).isFalse();
    }

    @Test
    void testNested() {
        var request = buildRequest(
                Map.of(),
                "",
                Map.of(
                        "some_message", new String[] {"x1"},
                        "requestMessage", new String[] {"y1"},
                        "nested.some_message", new String[] {"x2"},
                        "nested.requestMessage", new String[] {"y2"},
                        "nested.nested.some_message", new String[] {"x3"},
                        "nested.nested.repeated_string", new String[] {"a3", "b3"}));

        assertThat(request.getSomeMessage()).isEqualTo("x1");
        assertThat(request.getRequestMessage()).isEqualTo("y1");
        assertThat(request.hasNested()).isTrue();
        assertThat(request.getNested().getSomeMessage()).isEqualTo("x2");
        assertThat(request.getNested().getRequestMessage()).isEqualTo("y2");
        assertThat(request.getNested().hasNested()).isTrue();
        assertThat(request.getNested().getNested().getSomeMessage()).isEqualTo("x3");
        assertThat(request.getNested().getNested().getRepeatedStringList()).containsExactly("a3", "b3");
    }

    @Test
    void testRepeated() {
        var request = buildRequest(
                Map.of(),
                "",
                Map.of(
                        "repeated_message.some_message", new String[] {"x2"},
                        "repeated_message.requestMessage", new String[] {"y2"},
                        "repeated_string", new String[] {"v1", "v2"}));

        assertThat(request.getRepeatedMessageList()).isEmpty();
        assertThat(request.getRepeatedStringList()).containsExactly("v1", "v2");

        // test empty array
        request = buildRequest(Map.of(), "", Map.of("repeated_string", new String[] {}));

        assertThat(request.getRepeatedMessageList()).isEmpty();
        assertThat(request.getRepeatedStringList()).isEmpty();
    }

    @Test
    void testEnum() {
        // use string
        testEnumParsing("V1", transcoding.TranscoderTest.SimpleRequest.Enum.V1);
        testEnumParsing("V2", transcoding.TranscoderTest.SimpleRequest.Enum.V2);
        testEnumParsing(" ", transcoding.TranscoderTest.SimpleRequest.Enum.ENUM_UNSPECIFIED);
        assertThatCode(() -> testEnumParsing("NON", transcoding.TranscoderTest.SimpleRequest.Enum.ENUM_UNSPECIFIED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Can't parse enum value 'NON' for field 'enum'");

        // use number
        testEnumParsing("0", transcoding.TranscoderTest.SimpleRequest.Enum.ENUM_UNSPECIFIED);
        testEnumParsing("1", transcoding.TranscoderTest.SimpleRequest.Enum.V1);
        testEnumParsing("2", transcoding.TranscoderTest.SimpleRequest.Enum.V2);
        assertThatCode(() -> testEnumParsing("3", transcoding.TranscoderTest.SimpleRequest.Enum.ENUM_UNSPECIFIED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Can't parse enum value '3' for field 'enum'");
    }

    @Test
    void testComplexBody() {
        var body = """
                {
                    "requestMessage": "Hi",
                    "some_message": "Hi",
                    "nested": {
                        "requestMessage": "Hi",
                        "some_message": "Hi",
                        "nested": {
                            "requestMessage": "Hi",
                            "some_message": "Hi",
                            "repeated_string": ["a", "b"]
                        }
                    },
                    "repeated_string": ["a", "b"],
                    "repeated_message": [
                        {
                            "requestMessage": "Hi",
                            "some_message": "Hi"
                        },
                        {
                            "requestMessage": "Hi",
                            "some_message": "Hi"
                        }
                    ],
                    "enum": "V1",
                    "int32_wrapper": 1
                }""";

        var request = buildRequest(
                Map.of(), body, Map.of(), HttpRule.newBuilder().setBody("*").build());

        assertThat(request.getRequestMessage()).isEqualTo("Hi");
        assertThat(request.getSomeMessage()).isEqualTo("Hi");
        assertThat(request.hasNested()).isTrue();
        assertThat(request.getNested().getRequestMessage()).isEqualTo("Hi");
        assertThat(request.getNested().getSomeMessage()).isEqualTo("Hi");
        assertThat(request.getNested().hasNested()).isTrue();
        assertThat(request.getNested().getNested().getRequestMessage()).isEqualTo("Hi");
        assertThat(request.getNested().getNested().getSomeMessage()).isEqualTo("Hi");
        assertThat(request.getNested().getNested().getRepeatedStringList()).containsExactly("a", "b");
        assertThat(request.getRepeatedStringList()).containsExactly("a", "b");
        assertThat(request.getRepeatedMessageList()).hasSize(2);
        assertThat(request.getRepeatedMessage(0).getRequestMessage()).isEqualTo("Hi");
        assertThat(request.getRepeatedMessage(0).getSomeMessage()).isEqualTo("Hi");
        assertThat(request.getRepeatedMessage(1).getRequestMessage()).isEqualTo("Hi");
        assertThat(request.getRepeatedMessage(1).getSomeMessage()).isEqualTo("Hi");
        assertThat(request.getEnum()).isEqualTo(transcoding.TranscoderTest.SimpleRequest.Enum.V1);
        assertThat(request.getInt32Wrapper().getValue()).isEqualTo(1);
    }

    private static transcoding.TranscoderTest.SimpleRequest buildRequest(
            Map<String, String> pathVariables, String body, Map<String, String[]> parameterMap) {
        return buildRequest(pathVariables, body, parameterMap, HttpRule.getDefaultInstance());
    }

    @SneakyThrows
    private static transcoding.TranscoderTest.SimpleRequest buildRequest(
            Map<String, String> pathVariables, String body, Map<String, String[]> parameterMap, HttpRule httpRule) {
        Transcoder transcoder = Transcoder.create(new Transcoder.Variable(
                body != null ? ByteString.copyFrom(body.getBytes(StandardCharsets.UTF_8)) : ByteString.EMPTY,
                parameterMap,
                pathVariables));

        // body is empty, body is ignored
        var builder = transcoding.TranscoderTest.SimpleRequest.newBuilder();
        transcoder.into(builder, httpRule);
        return builder.build();
    }

    private static void testEnumParsing(String enumValue, transcoding.TranscoderTest.SimpleRequest.Enum expectedEnum) {
        var request = buildRequest(Map.of(), "", Map.of("enum", new String[] {enumValue}));

        assertThat(request.getEnum()).isEqualTo(expectedEnum);
    }
}
