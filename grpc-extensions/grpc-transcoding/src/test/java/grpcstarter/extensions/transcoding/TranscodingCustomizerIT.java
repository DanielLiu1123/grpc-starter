package grpcstarter.extensions.transcoding;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import com.google.api.HttpRule;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import transcoding.SimpleServiceGrpc;
import transcoding.TranscoderTest.SimpleRequest;
import transcoding.TranscoderTest.SimpleResponse;

/**
 * Integration tests for {@link TranscodingCustomizer}.
 */
class TranscodingCustomizerIT {

    final RestTestClient client = RestTestClient.bindToServer().build();

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testPathPrefixCustomizer(String webType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(PathPrefixConfig.class)
                .web(WebApplicationType.valueOf(webType))
                .properties("server.port=" + port)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .run()) {

            // Test that original path (without prefix) returns NOT_FOUND
            var respOriginal = client.post()
                    .uri("http://localhost:%d/v1/unaryrpc".formatted(port))
                    .body("{\"requestMessage\":\"World!\"}")
                    .exchange();
            respOriginal.expectStatus().isEqualTo(NOT_FOUND);

            // Test that prefixed path works
            var respPrefixed = client.post()
                    .uri("http://localhost:%d/api/v1/unaryrpc".formatted(port))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"requestMessage\":\"World!\"}")
                    .exchange();
            respPrefixed.expectStatus().isEqualTo(OK);
            respPrefixed.expectBody(String.class).isEqualTo("{\"responseMessage\":\"Hello World!\"}");
        }
    }

    /**
     * Test TranscodingCustomizer with auto-mapping enabled.
     * Customizers are applied to auto-mapped routes, so the customized path should work.
     */
    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testCustomizerWithAutoMapping(String webType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(AutoMappingConfig.class)
                .web(WebApplicationType.valueOf(webType))
                .properties("server.port=" + port)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .properties("grpc.transcoding.auto-mapping=true")
                .run()) {

            // Original auto-mapped path should NOT work
            var respNoPrefix = client.post()
                    .uri("http://localhost:%d/transcoding.SimpleService/UseAnotherPackageRequestRpc".formatted(port))
                    .body("{}")
                    .exchange();
            respNoPrefix.expectStatus().isEqualTo(NOT_FOUND);

            // Customized auto-mapped path (with /api prefix) should work
            var respWithPrefix = client.post()
                    .uri("http://localhost:%d/api/transcoding.SimpleService/UseAnotherPackageRequestRpc"
                            .formatted(port))
                    .body("{}")
                    .exchange();
            respWithPrefix.expectStatus().isEqualTo(OK);
            respWithPrefix.expectBody(String.class).isEqualTo("\"Hello\"");
        }
    }

    /**
     * Configuration with a customizer that adds "/api" prefix to all paths.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class PathPrefixConfig {

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
        TranscodingCustomizer pathPrefixCustomizer() {
            return (httpRule, descriptor) -> {
                String prefix = "/api";
                HttpRule.Builder builder = httpRule.toBuilder();
                switch (httpRule.getPatternCase()) {
                    case GET -> builder.setGet(prefix + httpRule.getGet());
                    case POST -> builder.setPost(prefix + httpRule.getPost());
                    case PUT -> builder.setPut(prefix + httpRule.getPut());
                    case DELETE -> builder.setDelete(prefix + httpRule.getDelete());
                    case PATCH -> builder.setPatch(prefix + httpRule.getPatch());
                    default -> {}
                }
                return builder.build();
            };
        }
    }

    /**
     * Configuration for testing customizer with auto-mapping enabled.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class AutoMappingConfig {

        @Bean
        SimpleServiceGrpc.SimpleServiceImplBase simpleService() {
            return new SimpleServiceGrpc.SimpleServiceImplBase() {
                @Override
                public void useAnotherPackageRequestRpc(
                        com.google.protobuf.Empty request,
                        StreamObserver<com.google.protobuf.StringValue> responseObserver) {
                    responseObserver.onNext(com.google.protobuf.StringValue.newBuilder()
                            .setValue("Hello")
                            .build());
                    responseObserver.onCompleted();
                }
            };
        }

        @Bean
        TranscodingCustomizer autoMappingPrefixCustomizer() {
            return (httpRule, descriptor) -> {
                String prefix = "/api";
                HttpRule.Builder builder = httpRule.toBuilder();
                switch (httpRule.getPatternCase()) {
                    case GET -> builder.setGet(prefix + httpRule.getGet());
                    case POST -> builder.setPost(prefix + httpRule.getPost());
                    case PUT -> builder.setPut(prefix + httpRule.getPut());
                    case DELETE -> builder.setDelete(prefix + httpRule.getDelete());
                    case PATCH -> builder.setPatch(prefix + httpRule.getPatch());
                    default -> {}
                }
                return builder.build();
            };
        }
    }
}
