package grpcstarter.extensions.transcoding;

import static grpcstarter.server.GrpcContextKeys.ResponseMetadataModifier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import transcoding.SimpleServiceGrpc;
import transcoding.TranscoderTest;

class JsonTranscoderIT {

    final RestTestClient client = RestTestClient.bindToServer().build();

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testTranscoderJson(String webType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.port=0")
                .web(WebApplicationType.valueOf(webType))
                .run()) {

            // path alias
            var resp = client.post()
                    .uri("http://localhost:" + port + "/v1/unaryrpc")
                    .body(
                            """
                            {
                                "requestMessage": "Hi"
                            }
                            """)
                    .exchange();

            resp.expectStatus().isEqualTo(HttpStatus.OK);
            resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
            resp.expectHeader().valueEquals("request-id", "001");
            resp.expectBody(String.class).isEqualTo("""
                    {"responseMessage":"Hi, Hi"}""");

            // Additional path alias
            resp = client.get()
                    .uri("http://localhost:" + port + "/v1/unaryrpc/Hi")
                    .exchange();
            resp.expectStatus().isEqualTo(HttpStatus.OK);
            resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
            resp.expectHeader().valueEquals("request-id", "001");
            resp.expectBody(String.class).isEqualTo("""
                    {"responseMessage":"Hi, Hi"}""");

            // wrapper type parses as JSON is simple value format
            // google.protobuf.StringValue convert to JSON, the result is "foo", not {"value":"foo"}
            resp = client.post()
                    .uri("http://localhost:" + port + "/v1/unaryrpc")
                    .body(
                            """
                            {
                                "int32_wrapper": 1
                            }
                            """)
                    .exchange();

            resp.expectStatus().isEqualTo(HttpStatus.OK);

            // test wrong format
            resp = client.post()
                    .uri("http://localhost:" + port + "/v1/unaryrpc")
                    .body(
                            """
                            {
                                "int32_wrapper": {
                                    "value": 1
                                }
                            }
                            """)
                    .exchange();
            resp.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseAnotherPackageRequestMessage(String webType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.port=0")
                .web(WebApplicationType.valueOf(webType))
                .run()) {

            var resp = client.post()
                    .uri("http://localhost:" + port + "/transcoding.SimpleService/UseAnotherPackageRequestRpc")
                    .exchange();

            resp.expectStatus().isEqualTo(HttpStatus.OK);
            resp.expectHeader().contentType("text/plain;charset=UTF-8");
            resp.expectBody(String.class).isEqualTo("\"Hello\"");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseSubMessageRequestRpc(String webType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.port=0")
                .web(WebApplicationType.valueOf(webType))
                .run()) {

            var resp = client.post()
                    .uri("http://localhost:" + port + "/transcoding.SimpleService/UseSubMessageRequestRpc")
                    .body("""
                            {"message":"Hello"}""")
                    .exchange();

            resp.expectStatus().isEqualTo(HttpStatus.OK);
            resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
            resp.expectBody(String.class).isEqualTo("""
                    {"message":"Hello"}""");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseRandomPort_thenTranscodingWorks(String webType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.port=0")
                .web(WebApplicationType.valueOf(webType))
                .run()) {

            var resp = client.post()
                    .uri("http://localhost:" + port + "/v1/unaryrpc")
                    .body(
                            """
                            {
                                "requestMessage": "Hi"
                            }
                            """)
                    .exchange();

            resp.expectStatus().isEqualTo(HttpStatus.OK);
            resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
            resp.expectBody(String.class).value(body -> {
                assertThat(body).isNotBlank();
                assertThat(body).isEqualTo("""
                        {"responseMessage":"Hi, Hi"}""");
            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testUseInProcess_thenTranscodingWorks(String webType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .web(WebApplicationType.valueOf(webType))
                .run()) {

            var resp = client.post()
                    .uri("http://localhost:" + port + "/v1/unaryrpc")
                    .body(
                            """
                            {
                                "requestMessage": "Hi"
                            }
                            """)
                    .exchange();

            resp.expectStatus().isEqualTo(HttpStatus.OK);
            resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
            resp.expectBody(String.class).value(body -> {
                assertThat(body).isNotBlank();
                assertThat(body).isEqualTo("""
                        {"responseMessage":"Hi, Hi"}""");
            });
        }
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
