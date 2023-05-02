package com.freemanan.starter.grpc.server;

import com.freemanan.starter.grpc.server.extension.debug.Debug;
import com.freemanan.starter.grpc.server.extension.exceptionhandling.ExceptionHandling;
import com.freemanan.starter.grpc.server.extension.healthcheck.HealthCheck;
import com.freemanan.starter.grpc.server.extension.validation.Validation;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Server.class)
@ConditionalOnProperty(prefix = GrpcServerProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerAutoConfiguration {

    @Bean
    public GrpcServer grpcServer(
            GrpcServerProperties properties,
            ObjectProvider<BindableService> services,
            ObjectProvider<ServerInterceptor> interceptors,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        return new GrpcServer(properties, services, interceptors, customizers);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({Debug.class, Validation.class, HealthCheck.class, ExceptionHandling.class})
    static class Extensions {}
}
