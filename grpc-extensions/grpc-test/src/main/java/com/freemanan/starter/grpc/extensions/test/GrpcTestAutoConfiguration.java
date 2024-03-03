package com.freemanan.starter.grpc.extensions.test;

import com.freemanan.starter.grpc.server.GrpcServerStartedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(GrpcServerStartedEvent.class) // server starter must be in classpath
@ConditionalOnProperty(prefix = GrpcTestProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcTestProperties.class)
public class GrpcTestAutoConfiguration {

    @Bean
    static GrpcTestBeanPostProcessor grpcTestBeanPostProcessor() {
        return new GrpcTestBeanPostProcessor();
    }
}
