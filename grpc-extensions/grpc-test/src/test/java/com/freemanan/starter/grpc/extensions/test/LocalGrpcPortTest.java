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
@SpringBootTest(classes = LocalGrpcPortTest.Cfg.class)
class LocalGrpcPortTest {

    @LocalGrpcPort
    int port;

    @Test
    void testLocalGrpcPort() {
        assertThat(port).isEqualTo(-1);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
