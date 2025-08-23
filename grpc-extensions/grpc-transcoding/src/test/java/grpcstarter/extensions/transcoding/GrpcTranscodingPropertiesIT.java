package grpcstarter.extensions.transcoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import transcoding.AddWhiteSpaceServiceGrpc;
import transcoding.AddWhitespaceTest;
import transcoding.PrintEnumServiceGrpc;
import transcoding.PrintEnumTest.PrintEnumRequest;
import transcoding.PrintEnumTest.PrintEnumResponse;

/**
 * {@link GrpcTranscodingProperties} tester.
 */
class GrpcTranscodingPropertiesIT {

    /**
     * {@link GrpcTranscodingProperties#endpoint}
     */
    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testEndpoint_whenUsingWrongEndpoint(String webType) {
        int httpPort = findAvailableTcpPort();
        int grpcPort = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .web(WebApplicationType.valueOf(webType))
                .properties("server.port=" + httpPort)
                .properties("grpc.server.port=" + grpcPort)
                .properties("grpc.transcoding.endpoint=localhost:" + (grpcPort - 1))
                .run()) {

            var rest = new TestRestTemplate();

            var response = rest.postForEntity(
                    "http://localhost:%d/grpc.testing.SimpleService/UnaryRpc".formatted(httpPort),
                    "{\"requestMessage\":\"World!\"}",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);
        }
    }

    /**
     * {@link GrpcTranscodingProperties#autoMapping}
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAutoMapping(boolean autoMapping) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .properties("grpc.transcoding.auto-mapping=" + autoMapping)
                .run()) {

            var rest = new TestRestTemplate();

            var response = rest.postForEntity(
                    "http://localhost:%d/grpc.testing.SimpleService/UnaryRpc".formatted(port),
                    "{\"requestMessage\":\"World!\"}",
                    String.class);

            if (autoMapping) {
                assertThat(response.getStatusCode()).isEqualTo(OK);
                assertThat(response.getBody()).isEqualTo("{\"responseMessage\":\"Hello World!\"}");
            } else {
                assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
            }
        }
    }

    /**
     * {@link GrpcTranscodingProperties.PrintOptions#addWhitespace}
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAddWhitespace(boolean addWhitespace) {
        int port = findAvailableTcpPort();
        var ctx = new SpringApplicationBuilder(GrpcTranscodingPropertiesIT.Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .properties("grpc.transcoding.print-options.add-whitespace=" + addWhitespace)
                .run();

        var rest = new TestRestTemplate();

        var response = rest.postForEntity(
                "http://localhost:%d/transcoding.AddWhiteSpaceService/AddWhiteSpace".formatted(port),
                null,
                String.class);

        if (addWhitespace) {
            assertThat(response.getStatusCode()).isEqualTo(OK);
            assertThat(response.getBody())
                    .isEqualTo(
                            """
                    {
                      "text": "Hello World!"
                    }""");
        } else {
            assertThat(response.getStatusCode()).isEqualTo(OK);
            assertThat(response.getBody()).isEqualTo("""
                    {"text":"Hello World!"}""");
        }

        ctx.close();
    }

    /**
     * {@link GrpcTranscodingProperties.PrintOptions#alwaysPrintEnumsAsInts}
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAlwaysPrintEnumsAsInts(boolean alwaysPrintEnumsAsInts) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .properties("grpc.transcoding.print-options.always-print-enums-as-ints=" + alwaysPrintEnumsAsInts)
                .run()) {

            var rest = new TestRestTemplate();

            var response = rest.postForEntity(
                    "http://localhost:%d/transcoding.PrintEnumService/PrintEnum".formatted(port), null, String.class);

            if (alwaysPrintEnumsAsInts) {
                assertThat(response.getStatusCode()).isEqualTo(OK);
                assertThat(response.getBody()).isEqualTo("""
                        {"enum":1}""");
            } else {
                assertThat(response.getStatusCode()).isEqualTo(OK);
                assertThat(response.getBody()).isEqualTo("""
                        {"enum":"V1"}""");
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {

        @Bean
        SimpleServiceGrpc.SimpleServiceImplBase simpleService() {
            return new SimpleServiceGrpc.SimpleServiceImplBase() {
                @Override
                public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                    responseObserver.onNext(SimpleResponse.newBuilder()
                            .setResponseMessage("Hello " + request.getRequestMessage())
                            .build());
                    responseObserver.onCompleted();
                }
            };
        }

        @Bean
        AddWhiteSpaceServiceGrpc.AddWhiteSpaceServiceImplBase addWhiteSpaceService() {
            return new AddWhiteSpaceServiceGrpc.AddWhiteSpaceServiceImplBase() {
                @Override
                public void addWhiteSpace(
                        AddWhitespaceTest.AddWhiteSpaceRequest request,
                        StreamObserver<AddWhitespaceTest.AddWhiteSpaceResponse> responseObserver) {
                    responseObserver.onNext(AddWhitespaceTest.AddWhiteSpaceResponse.newBuilder()
                            .setText("Hello World!")
                            .build());
                    responseObserver.onCompleted();
                }
            };
        }

        @Bean
        PrintEnumServiceGrpc.PrintEnumServiceImplBase printEnumService() {
            return new PrintEnumServiceGrpc.PrintEnumServiceImplBase() {
                @Override
                public void printEnum(PrintEnumRequest request, StreamObserver<PrintEnumResponse> responseObserver) {
                    responseObserver.onNext(PrintEnumResponse.newBuilder()
                            .setEnum(PrintEnumResponse.Enum.V1)
                            .build());
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
