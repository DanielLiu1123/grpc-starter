package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import grpcstarter.server.GrpcServerProperties;
import io.grpc.health.v1.HealthGrpc;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for SSL Bundle support in gRPC client configuration.
 *
 * @author Freeman
 */
class ClientSslBundleIT {

    @Test
    void testSslBundleConfiguration_whenSslBundleNotFound() {
        assertThatExceptionOfType(BeanCreationException.class)
                .isThrownBy(() -> {
                    try (var ignored = new SpringApplicationBuilder(Cfg.class)
                            .properties(GrpcServerProperties.PREFIX + ".enabled=false")
                            .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                            .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                            .properties(GrpcClientProperties.PREFIX + ".channels[0].ssl-bundle=nonexistent")
                            .properties(GrpcClientProperties.PREFIX + ".channels[0].services[0]=grpc.health.**.Health")
                            .run()) {
                        // This should fail during context initialization
                    }
                })
                .havingCause()
                .withMessageContaining("SSL bundle name 'nonexistent' cannot be found");
    }

    @Test
    void testPlaintextConfiguration_whenNoSslBundle() {
        // When ssl-bundle is not configured, should use plaintext
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".enabled=false")
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                .properties(GrpcClientProperties.PREFIX + ".authority=localhost:9090")
                .run()) {

            // This should not throw an exception during bean creation
            // The actual connection failure will happen when making RPC calls
            assertThatNoException().isThrownBy(() -> ctx.getBean(HealthGrpc.HealthBlockingStub.class)
                    .getChannel());
        }
    }

    @Test
    void testInProcessConfiguration_ignoresSslBundle() {
        String name = UUID.randomUUID().toString();

        // In-process channels should ignore SSL bundle configuration
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .properties(GrpcClientProperties.InProcess.PREFIX + ".name=" + name)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                .properties(GrpcClientProperties.PREFIX + ".authority=localhost:9090")
                .run()) {

            assertThatNoException().isThrownBy(() -> ctx.getBean(HealthGrpc.HealthBlockingStub.class)
                    .getChannel());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
