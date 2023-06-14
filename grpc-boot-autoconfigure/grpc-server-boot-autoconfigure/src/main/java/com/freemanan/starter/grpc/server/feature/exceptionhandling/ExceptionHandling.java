package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = GrpcServerProperties.ExceptionHandling.PREFIX, name = "enabled", matchIfMissing = true)
public class ExceptionHandling {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = GrpcServerProperties.ExceptionHandling.PREFIX, name = "use-default")
    public DefaultGrpcExceptionHandler defaultGrpcExceptionHandler() {
        return new DefaultGrpcExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionHandlingServerInterceptor grpcExceptionHandlingServerInterceptor(
            ObjectProvider<GrpcExceptionHandler> exceptionHandlers,
            ObjectProvider<GrpcUnhandledExceptionProcessor> unhandledExceptionProcessors) {
        return new ExceptionHandlingServerInterceptor(exceptionHandlers, unhandledExceptionProcessors);
    }
}
