package com.freemanan.starter.grpc.extensions.test;

import com.freemanan.starter.grpc.server.GrpcServerStartedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(GrpcServerStartedEvent.class) // server starter must be in classpath
public class GrpcTestAutoConfiguration {

    @Bean
    public static GrpcPortBeanPostProcessor grpcPortBeanPostProcessor() {
        return new GrpcPortBeanPostProcessor();
    }
}
