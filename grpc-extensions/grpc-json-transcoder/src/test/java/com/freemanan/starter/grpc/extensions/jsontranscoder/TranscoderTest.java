package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.api.HttpRule;
import java.util.Map;
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
    }

    @Test
    void testNested() {
        var request = buildRequest(
                null,
                null,
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
                null,
                null,
                Map.of(
                        "repeated_message.some_message", new String[] {"x2"},
                        "repeated_message.requestMessage", new String[] {"y2"},
                        "repeated_string", new String[] {"v1", "v2"}));

        assertThat(request.getRepeatedMessageList()).isEmpty();
        assertThat(request.getRepeatedStringList()).containsExactly("v1", "v2");

        // test empty array
        request = buildRequest(null, null, Map.of("repeated_string", new String[] {}));

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

    private static transcoding.TranscoderTest.SimpleRequest buildRequest(
            Map<String, String> pathVariables, String body, Map<String, String[]> parameterMap) {
        Transcoder transcoder = Transcoder.create(
                new Transcoder.Variable(body != null ? body.getBytes(UTF_8) : null, parameterMap, pathVariables));

        // body is empty, body is ignored
        var builder = transcoding.TranscoderTest.SimpleRequest.newBuilder();
        transcoder.into(builder, HttpRule.getDefaultInstance());
        return builder.build();
    }

    private static void testEnumParsing(String enumValue, transcoding.TranscoderTest.SimpleRequest.Enum expectedEnum) {
        var request = buildRequest(null, null, Map.of("enum", new String[] {enumValue}));

        assertThat(request.getEnum()).isEqualTo(expectedEnum);
    }
}
