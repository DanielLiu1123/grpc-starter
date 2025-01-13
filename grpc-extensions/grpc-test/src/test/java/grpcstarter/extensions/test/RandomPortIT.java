package grpcstarter.extensions.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Test when {@code grpc.test.server.port-type} set to {@link GrpcTestProperties.PortType#RANDOM_PORT},
 * the grpc server will start with random port.
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
