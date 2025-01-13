package grpcstarter.extensions.test;

import static org.assertj.core.api.Assertions.assertThat;

import grpcstarter.extensions.test.GrpcTestProperties.PortType;
import grpcstarter.server.GrpcServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Test when {@code grpc.test.server.port-type} set to {@link PortType#NONE},
 * the grpc server will not start.
 *
 * @author Freeman
 */
@SpringBootTest(
        classes = NonePortIT.Cfg.class,
        properties = {
            "grpc.test.server.port-type=none",
        })
class NonePortIT {

    @Autowired
    ObjectProvider<GrpcServer> grpcServerProvider;

    @Test
    void whenPortTypeIsNone_thenGrpcServerNotStart() {
        assertThat(grpcServerProvider).isEmpty();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
