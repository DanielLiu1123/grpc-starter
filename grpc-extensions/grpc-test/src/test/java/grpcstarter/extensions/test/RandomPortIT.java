package grpcstarter.extensions.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * {@link LocalGrpcPort} tester.
 *
 * @author Freeman
 */
@SpringBootTest(
        classes = RandomPortIT.Cfg.class,
        properties = {
            "grpc.test.server.port-type=random_port",
        })
class RandomPortIT {

    @LocalGrpcPort
    int port;

    @Test
    void testRandomPort() {
        assertThat(port).isNotEqualTo(-1).isNotEqualTo(9090);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
