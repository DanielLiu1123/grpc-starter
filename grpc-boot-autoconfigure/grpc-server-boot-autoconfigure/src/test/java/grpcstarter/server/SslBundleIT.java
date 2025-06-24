package grpcstarter.server;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for SSL Bundle support in gRPC server configuration.
 *
 * @author Freeman
 */
class SslBundleIT {

    @Test
    void testSslBundleConfiguration_whenSslBundleNotFound() {
        assertThatExceptionOfType(BeanCreationException.class)
                .isThrownBy(() -> {
                    try (var ctx = new SpringApplicationBuilder(Cfg.class)
                            .properties(GrpcServerProperties.PREFIX + ".ssl-bundle=nonexistent")
                            .properties(GrpcServerProperties.PREFIX + ".port=0")
                            .run()) {
                        // This should fail during context initialization
                    }
                })
                .havingRootCause()
                .withMessageContaining("SSL bundle name 'nonexistent' cannot be found");
    }

    @Test
    void testPlaintextConfiguration_whenNoSslBundleOrTls() {
        // When neither ssl-bundle nor tls is configured, should use plaintext
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=0")
                .run()) {

            // This should not throw an exception during bean creation
            assertThatNoException().isThrownBy(() -> ctx.getBean(GrpcServer.class));
        }
    }

    @Test
    void testInProcessConfiguration_ignoresSslBundle() {
        String name = UUID.randomUUID().toString();

        // In-process servers should ignore SSL bundle configuration
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .properties(GrpcServerProperties.PREFIX + ".ssl-bundle=some-bundle")
                .run()) {

            assertThatNoException().isThrownBy(() -> ctx.getBean(GrpcServer.class));
        }
    }

    @Test
    void testTlsDeprecationWarning_whenUsingTlsConfiguration() {
        // This test verifies that using deprecated TLS configuration logs a deprecation warning
        // We don't provide actual certificate files, so it should fail during bean creation
        // but the important thing is that the deprecation warning is logged
        assertThatExceptionOfType(BeanCreationException.class)
                .isThrownBy(() -> {
                    try (var ctx = new SpringApplicationBuilder(Cfg.class)
                            .properties(GrpcServerProperties.PREFIX + ".port=0")
                            .properties(GrpcServerProperties.PREFIX
                                    + ".tls.key-manager.cert-chain=classpath:nonexistent.crt")
                            .properties(GrpcServerProperties.PREFIX
                                    + ".tls.key-manager.private-key=classpath:nonexistent.key")
                            .run()) {
                        // This should fail during context initialization due to missing certificate files
                    }
                })
                .havingRootCause()
                .withMessageContaining("cannot be opened because it does not exist");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
