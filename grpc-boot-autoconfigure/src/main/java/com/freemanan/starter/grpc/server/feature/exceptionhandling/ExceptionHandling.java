package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import com.freemanan.starter.grpc.GrpcProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = GrpcProperties.Server.ExceptionHandling.PREFIX, name = "enabled", matchIfMissing = true)
public class ExceptionHandling {

    @Bean
    @ConditionalOnMissingBean
    public ExceptionHandlingServerInterceptor grpcHandleExceptionServerInterceptor(
            ObjectProvider<ExceptionHandler> exceptionHandlers,
            ObjectProvider<UnhandledExceptionProcessor> unhandledExceptionProcessors) {
        return new ExceptionHandlingServerInterceptor(exceptionHandlers, unhandledExceptionProcessors);
    }
}
