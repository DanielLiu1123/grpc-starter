package com.freemanan.starter.grpc.extensions.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
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

    @Autowired
    private ApplicationContext ctx;

    @Test
    void testLocalGrpcPort() {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg implements ApplicationListener<ApplicationReadyEvent> {

        @LocalGrpcPort
        int port;

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            System.out.println("port = " + port);
        }
    }
}
