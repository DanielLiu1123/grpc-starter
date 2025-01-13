package grpcstarter.extensions.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import grpcstarter.server.GrpcServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Test when create {@link ApplicationContext} in test method, the port still use defined port, not in-process.
 *
 * @author Freeman
 */
class CreateApplicationContextInTestMethodIT {

    @Test
    void whenCreateApplicationContextInTestMethod_thenStillUseDefinedPort() {
        var port = findAvailableTcpPort();

        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.port=" + port)
                .run()) {
            assertThat(ctx.getBean(GrpcServer.class).getPort()).isEqualTo(port);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
