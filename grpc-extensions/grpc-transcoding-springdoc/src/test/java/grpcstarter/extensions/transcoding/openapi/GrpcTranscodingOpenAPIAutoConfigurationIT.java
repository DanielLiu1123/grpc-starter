package grpcstarter.extensions.transcoding.openapi;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for {@link GrpcTranscodingOpenAPIAutoConfiguration}.
 */
class GrpcTranscodingOpenAPIAutoConfigurationIT {

    @ParameterizedTest
    @MethodSource("enabledScenarios")
    void testAutoConfigurationEnabled(List<String> properties) {
        try (var context = SpringApplication.run(Cfg.class, properties.toArray(String[]::new))) {

            assertThatCode(() -> context.getBean(GrpcTranscodingOpenAPIAutoConfiguration.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> context.getBean(GrpcTranscodingOpenApiCustomizer.class))
                    .doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @MethodSource("disabledScenarios")
    void testAutoConfigurationDisabled(List<String> properties) {
        try (var context = SpringApplication.run(Cfg.class, properties.toArray(String[]::new))) {
            assertThatCode(() -> context.getBean(GrpcTranscodingOpenAPIAutoConfiguration.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    static Stream<Arguments> enabledScenarios() {
        return Stream.of(
                Arguments.of(List.of("--grpc.server.port=0")),
                Arguments.of(List.of(
                        "--grpc.server.port=0",
                        "--grpc.transcoding.enabled=true",
                        "--springdoc.api-docs.enabled=true",
                        "--springdoc-bridge.protobuf.enabled=true")));
    }

    static Stream<Arguments> disabledScenarios() {
        return Stream.of(
                Arguments.of(List.of(
                        "--grpc.server.port=0",
                        "--grpc.transcoding.enabled=false",
                        "--springdoc.api-docs.enabled=true",
                        "--springdoc-bridge.protobuf.enabled=true")),
                Arguments.of(List.of(
                        "--grpc.server.port=0",
                        "--grpc.transcoding.enabled=true",
                        "--springdoc.api-docs.enabled=false",
                        "--springdoc-bridge.protobuf.enabled=true")),
                Arguments.of(List.of(
                        "--grpc.server.port=0",
                        "--grpc.transcoding.enabled=true",
                        "--springdoc.api-docs.enabled=true",
                        "--springdoc-bridge.protobuf.enabled=false")),
                Arguments.of(List.of(
                        "--grpc.server.enabled=false",
                        "--grpc.transcoding.enabled=true",
                        "--springdoc.api-docs.enabled=true",
                        "--springdoc-bridge.protobuf.enabled=true")));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
