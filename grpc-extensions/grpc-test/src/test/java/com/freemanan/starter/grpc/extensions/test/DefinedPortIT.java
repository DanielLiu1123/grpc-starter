package com.freemanan.starter.grpc.extensions.test;

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
        classes = DefinedPortIT.Cfg.class,
        properties = {"grpc.server.port=50054", "grpc.test.server.port-type=DEFINED_PORT"})
class DefinedPortIT {

    @LocalGrpcPort
    int port;

    @Test
    void testDefinedPort() {
        assertThat(port).isEqualTo(50054);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
