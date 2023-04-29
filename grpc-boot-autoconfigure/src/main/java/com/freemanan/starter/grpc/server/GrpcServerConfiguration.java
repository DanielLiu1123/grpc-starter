package com.freemanan.starter.grpc.server;

import com.freemanan.starter.grpc.GrpcProperties;
import com.freemanan.starter.grpc.server.feature.debug.Debug;
import com.freemanan.starter.grpc.server.feature.exceptionhandling.ExceptionHandling;
import com.freemanan.starter.grpc.server.feature.healthcheck.HealthCheck;
import com.freemanan.starter.grpc.server.feature.validation.Validation;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Server.class)
@ConditionalOnProperty(prefix = GrpcProperties.Server.PREFIX, name = "enabled", matchIfMissing = true)
@Import({Debug.class, Validation.class, HealthCheck.class, ExceptionHandling.class})
public class GrpcServerConfiguration {

    @Bean
    public GrpcServer grpcServer(
            GrpcProperties properties,
            ObjectProvider<BindableService> services,
            ObjectProvider<ServerInterceptor> interceptors,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        return new GrpcServer(properties.getServer(), services, interceptors, customizers);
    }
}
