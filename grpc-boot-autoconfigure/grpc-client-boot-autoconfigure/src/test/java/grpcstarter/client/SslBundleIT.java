package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import grpcstarter.server.GrpcServerProperties;
import io.grpc.health.v1.HealthGrpc;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for SSL Bundle support in gRPC client configuration.
 *
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class SslBundleIT {

    @Test
    void testSslBundleConfiguration_whenSslBundleNotFound() {
        assertThatThrownBy(() -> {
            try (var ctx = new SpringApplicationBuilder(Cfg.class)
                    .properties(GrpcServerProperties.PREFIX + ".enabled=false")
                    .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                    .properties(GrpcClientProperties.PREFIX + ".authority=localhost:9090")
                    .properties(GrpcClientProperties.PREFIX + ".ssl-bundle=nonexistent")
                    .run()) {

                var stub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);
                // Force channel creation by accessing the channel
                stub.getChannel();
            }
        }).isInstanceOf(BeanCreationException.class);
    }

    @Test
    void testChannelSpecificSslBundle() {
        assertThatThrownBy(() -> {
            try (var ctx = new SpringApplicationBuilder(Cfg.class)
                    .properties(GrpcServerProperties.PREFIX + ".enabled=false")
                    .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                    .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                    .properties(GrpcClientProperties.PREFIX + ".channels[0].ssl-bundle=nonexistent")
                    .properties(GrpcClientProperties.PREFIX + ".channels[0].services[0]=grpc.health.v1.Health")
                    .run()) {

                var stub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);
                stub.getChannel();
            }
        }).isInstanceOf(BeanCreationException.class);
    }

    @Test
    void testPlaintextConfiguration_whenNoSslBundleOrTls() {
        // When neither ssl-bundle nor tls is configured, should use plaintext
        assertThatCode(() -> {
            try (var ctx = new SpringApplicationBuilder(Cfg.class)
                    .properties(GrpcServerProperties.PREFIX + ".enabled=false")
                    .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                    .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                    .properties(GrpcClientProperties.PREFIX + ".channels[0].services[0]=grpc.health.v1.Health")
                    .run()) {

                // This should not throw an exception during bean creation
                // The actual connection failure will happen when making RPC calls
                var stub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);
                assertThat(stub).isNotNull();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testInProcessConfiguration_ignoresSslBundle() {
        String name = UUID.randomUUID().toString();
        
        // In-process channels should ignore SSL bundle configuration
        assertThatCode(() -> {
            try (var ctx = new SpringApplicationBuilder(Cfg.class)
                    .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                    .properties(GrpcClientProperties.InProcess.PREFIX + ".name=" + name)
                    .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                    .properties(GrpcClientProperties.PREFIX + ".ssl-bundle=nonexistent")
                    .run()) {
                
                ctx.getBean(HealthGrpc.HealthBlockingStub.class);
            }
        }).doesNotThrowAnyException();
    }



    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
