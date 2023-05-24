package com.freemanan.starter.grpc.server;

import static com.freemanan.starter.grpc.server.Util.allInternalServices;

import com.freemanan.starter.grpc.server.feature.debug.Debug;
import com.freemanan.starter.grpc.server.feature.exceptionhandling.ExceptionHandling;
import com.freemanan.starter.grpc.server.feature.healthcheck.HealthCheck;
import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionOnGrpcServerEnabled
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GrpcServer grpcServer(
            GrpcServerProperties properties,
            ObjectProvider<BindableService> services,
            ObjectProvider<ServerInterceptor> interceptors,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        return properties.isEnableEmptyServer() || !allInternalServices(services)
                ? new DefaultGrpcServer(properties, services, interceptors, customizers)
                : new DummyGrpcServer();
    }

    @Configuration(proxyBeanMethods = false)
    @Import({Debug.class, HealthCheck.class, ExceptionHandling.class})
    static class Features {}
}
