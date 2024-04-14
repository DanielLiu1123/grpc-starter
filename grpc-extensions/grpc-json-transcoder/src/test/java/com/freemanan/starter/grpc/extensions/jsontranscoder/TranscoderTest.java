package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.api.HttpRule;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link Transcoder} tester.
 */
class TranscoderTest {

    @Test
    void testInto() {
        // path variables -> body -> parameters
        var body = """
                {"some_message": "x1", "requestMessage": "y1"}
                """;
        var parameters = Map.of(
                "some_message", new String[] {"x2"},
                "requestMessage", new String[] {"y2"});
        var pathVars = Map.of("requestMessage", "y3");
        Transcoder transcoder = new Transcoder(new Transcoder.Variable(body.getBytes(), parameters, pathVars));

        // body is empty, body is ignored
        var builder = transcoding.TranscoderTest.SimpleRequest.newBuilder();
        transcoder.into(builder, HttpRule.getDefaultInstance());
        var request = builder.build();

        assertThat(request.getSomeMessage()).isEqualTo("x2");
        assertThat(request.getRequestMessage()).isEqualTo("y3");
        assertThat(request.hasNested()).isFalse();

        // body is *, query parameters are ignored
        builder = transcoding.TranscoderTest.SimpleRequest.newBuilder();
        transcoder.into(builder, HttpRule.newBuilder().setBody("*").build());
        request = builder.build();

        assertThat(request.getSomeMessage()).isEqualTo("x1");
        assertThat(request.getRequestMessage()).isEqualTo("y3");
        assertThat(request.hasNested()).isFalse();

        // body is not blank, specific message fields will be set, query parameters are ignoredw
        builder = transcoding.TranscoderTest.SimpleRequest.newBuilder();
        transcoder.into(builder, HttpRule.newBuilder().setBody("nested").build());
        request = builder.build();

        assertThat(request.getSomeMessage()).isEmpty();
        assertThat(request.getRequestMessage()).isEqualTo("y3"); // from path variables
        assertThat(request.hasNested()).isTrue();
        assertThat(request.getNested().getSomeMessage()).isEqualTo("x1"); // from body
        assertThat(request.getNested().getRequestMessage()).isEqualTo("y1"); // from body
    }

    @Test
    void testSetNestedMessage() {
        // path variables -> body -> parameters
        var parameters = Map.of(
                "some_message", new String[] {"x2"},
                "requestMessage", new String[] {"y2"},
                "nested.some_message", new String[] {"x2"},
                "nested.requestMessage", new String[] {"y2"});
        var pathVars = Map.of("requestMessage", "y3");
        Transcoder transcoder = new Transcoder(new Transcoder.Variable(null, parameters, pathVars));

        // body is empty, body is ignored
        var builder = transcoding.TranscoderTest.SimpleRequest.newBuilder();
        transcoder.into(builder, HttpRule.getDefaultInstance());
        var request = builder.build();

        assertThat(request.getSomeMessage()).isEmpty();
        assertThat(request.getRequestMessage()).isEqualTo("y3");
        assertThat(request.hasNested()).isTrue();
        assertThat(request.getNested().getSomeMessage()).isEqualTo("x2");
        assertThat(request.getNested().getRequestMessage()).isEqualTo("y2");
    }
}
