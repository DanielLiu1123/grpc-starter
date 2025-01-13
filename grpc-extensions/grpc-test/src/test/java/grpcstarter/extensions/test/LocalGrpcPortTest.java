package grpcstarter.extensions.test;

import static org.assertj.core.api.Assertions.assertThat;

import grpcstarter.extensions.test.GrpcTestProperties.PortType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Test when not set {@code grpc.test.server.port-type}, the port type will use {@link PortType#IN_PROCESS} by default.
 *
 * @author Freeman
 */
@SpringBootTest(classes = LocalGrpcPortTest.Cfg.class, properties = "grpc.server.port=9091")
class LocalGrpcPortTest {

    @LocalGrpcPort
    int port;

    @InProcessName
    String inProcessName;

    @Autowired
    ApplicationContext ctx;

    @Test
    void whenNotSetPortType_thenUseInProcess() {
        assertThat(port).isEqualTo(-1);
        int p = ctx.getBean(Cfg.class).port;
        assertThat(p).isEqualTo(-1);

        assertThat(inProcessName).isNotBlank();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {
        @LocalGrpcPort
        int port;
    }
}
