package grpcstarter.extensions.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * {@link LocalGrpcPort} tester.
 *
 * @author Freeman
 */
@SpringBootTest(classes = LocalGrpcPortTest.Cfg.class, properties = "grpc.server.port=9091")
class LocalGrpcPortTest {

    @LocalGrpcPort
    int port;

    @Autowired
    ApplicationContext ctx;

    @Test
    void testAlwaysUsingRandomPort() {
        assertThat(port).isEqualTo(-1);
        int p = ctx.getBean(Cfg.class).port;
        assertThat(p).isEqualTo(-1);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {
        @LocalGrpcPort
        int port;
    }
}
