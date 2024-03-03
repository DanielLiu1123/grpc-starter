package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import com.freemanan.starter.grpc.server.feature.exceptionhandling.annotation.AnnotationBasedGrpcExceptionResolver;
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
    public AnnotationBasedGrpcExceptionResolver annotationBasedGrpcExceptionHandler() {
        return new AnnotationBasedGrpcExceptionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionHandlingServerInterceptor grpcExceptionHandlingServerInterceptor(
            ObjectProvider<GrpcExceptionResolver> exceptionHandlers,
            ObjectProvider<GrpcUnhandledExceptionProcessor> unhandledExceptionProcessors) {
        return new ExceptionHandlingServerInterceptor(exceptionHandlers, unhandledExceptionProcessors);
    }
}
