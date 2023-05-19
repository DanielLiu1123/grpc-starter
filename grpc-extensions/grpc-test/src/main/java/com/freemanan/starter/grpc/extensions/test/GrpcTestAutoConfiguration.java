package com.freemanan.starter.grpc.extensions.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
public class GrpcTestAutoConfiguration {

    @Bean
    public static GrpcPortListener grpcTestPortListener() {
        return new GrpcPortListener();
    }
}
