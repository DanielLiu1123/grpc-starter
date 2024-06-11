package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.TestUtil.randomPort;
import static grpcstarter.extensions.transcoding.TestUtil.restTemplate;
import static grpcstarter.server.GrpcContextKeys.ResponseMetadataModifier;
import static org.assertj.core.api.Assertions.assertThat;
import static transcoding.TranscoderTest.SimpleRequest;
import static transcoding.TranscoderTest.SimpleResponse;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import transcoding.SimpleServiceGrpc;
import transcoding.TranscoderTest;

class JsonTranscoderIT {

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testTranscoderJson(String webType) {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .web(WebApplicationType.valueOf(webType))
                .run();

        var client = restTemplate();

        // full path
        var resp = client.exchange(
                "http://localhost:" + port + "/transcoding.SimpleService/UnaryRpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                                {
                                    "requestMessage": "Hi"
                                }
                                """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody()).isEqualTo("""
                {"responseMessage":"Hi, Hi"}""");

        // path alias
        resp = client.exchange(
                "http://localhost:" + port + "/v1/unaryrpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                                {
                                    "requestMessage": "Hi"
                                }
                                """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getHeaders().get("request-id")).containsExactly("001");
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody()).isEqualTo("""
                {"responseMessage":"Hi, Hi"}""");

        // wrapper type parses as JSON is simple value format
        // google.protobuf.StringValue convert to JSON, the result is "foo", not {"value":"foo"}
        resp = client.exchange(
                "http://localhost:" + port + "/v1/unaryrpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                                {
                                    "int32_wrapper": 1
                                }
                                """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // test wrong format
        resp = client.exchange(
                "http://localhost:" + port + "/v1/unaryrpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                                {
                                    "int32_wrapper": {
                                        "value": 1
                                    }
                                }
                                """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseAnotherPackageRequestMessage(String webType) {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .web(WebApplicationType.valueOf(webType))
                .run();

        var client = restTemplate();

        var resp = client.postForEntity(
                "http://localhost:" + port + "/transcoding.SimpleService/UseAnotherPackageRequestRpc",
                null,
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).hasToString("text/plain;charset=UTF-8");
        assertThat(resp.getBody()).isEqualTo("Hello");

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseSubMessageRequestRpc(String webType) {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .web(WebApplicationType.valueOf(webType))
                .run();

        var client = restTemplate();

        var resp = client.postForEntity(
                "http://localhost:" + port + "/transcoding.SimpleService/UseSubMessageRequestRpc",
                new HttpEntity<>("""
                        {"message":"Hello"}"""),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).hasToString("application/json");
        assertThat(resp.getBody()).isEqualTo("""
                {"message":"Hello"}""");

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseRandomPort_thenTranscodingWorks(String webType) {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.port=0")
                .web(WebApplicationType.valueOf(webType))
                .run();

        var client = restTemplate();

        var resp = client.exchange(
                "http://localhost:" + port + "/transcoding.SimpleService/UnaryRpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                                {
                                    "requestMessage": "Hi"
                                }
                                """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody()).isEqualTo("""
                {"responseMessage":"Hi, Hi"}""");

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseInProcess_thenTranscodingWorks(String webType) {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .web(WebApplicationType.valueOf(webType))
                .run();

        var client = restTemplate();

        var resp = client.exchange(
                "http://localhost:" + port + "/transcoding.SimpleService/UnaryRpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                                {
                                    "requestMessage": "Hi"
                                }
                                """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody()).isEqualTo("""
                {"responseMessage":"Hi, Hi"}""");

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> ro) {
            if (request.getRequestMessage().startsWith("err")) {
                throw new IllegalArgumentException("invalid name: " + request.getRequestMessage());
            }

            ResponseMetadataModifier.addConsumer(
                    metadata -> metadata.put(Metadata.Key.of("request-id", Metadata.ASCII_STRING_MARSHALLER), "001"));

            ro.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hi, " + request.getRequestMessage())
                    .build());
            ro.onCompleted();
        }

        @Override
        public void serverStreamingRpc(SimpleRequest request, StreamObserver<SimpleResponse> ro) {
            ro.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hi, " + request.getRequestMessage())
                    .build());
            ro.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hi, " + request.getRequestMessage())
                    .build());
            ro.onCompleted();
        }

        @Override
        public void useAnotherPackageRequestRpc(Empty request, StreamObserver<StringValue> responseObserver) {
            responseObserver.onNext(StringValue.of("Hello"));
            responseObserver.onCompleted();
        }

        @Override
        public void useSubMessageRequestRpc(
                TranscoderTest.UseSubMessageRequestRpcRequest.SubMessage request,
                StreamObserver<TranscoderTest.UseSubMessageRequestRpcResponse.SubMessage> responseObserver) {
            responseObserver.onNext(TranscoderTest.UseSubMessageRequestRpcResponse.SubMessage.newBuilder()
                    .setMessage("Hello")
                    .build());
            responseObserver.onCompleted();
        }
    }
}
