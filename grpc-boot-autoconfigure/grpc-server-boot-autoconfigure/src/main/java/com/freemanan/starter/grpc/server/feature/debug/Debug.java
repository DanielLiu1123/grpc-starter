package com.freemanan.starter.grpc.server.feature.debug;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnMissingBean
    public ServerReflectionGrpc.ServerReflectionImplBase grpcProtoReflectionService() {
        return ((ProtoReflectionService) ProtoReflectionService.newInstance());
    }
}
