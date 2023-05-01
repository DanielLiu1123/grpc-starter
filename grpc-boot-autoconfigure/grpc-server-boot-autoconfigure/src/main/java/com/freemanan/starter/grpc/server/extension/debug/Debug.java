package com.freemanan.starter.grpc.server.extension.debug;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ProtoReflectionService.class)
@ConditionalOnProperty(prefix = GrpcServerProperties.Debug.PREFIX, name = "enabled")
public class Debug {

    @Bean
    public BindableService grpcProtoReflectionService() {
        return ProtoReflectionService.newInstance();
    }
}
