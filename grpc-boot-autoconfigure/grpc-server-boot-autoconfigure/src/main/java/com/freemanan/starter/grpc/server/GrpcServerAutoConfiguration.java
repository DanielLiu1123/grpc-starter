package com.freemanan.starter.grpc.server;

import static com.freemanan.starter.grpc.server.Util.allInternalServices;

import com.freemanan.starter.grpc.server.feature.channelz.Channelz;
import com.freemanan.starter.grpc.server.feature.exceptionhandling.ExceptionHandling;
import com.freemanan.starter.grpc.server.feature.health.Health;
import com.freemanan.starter.grpc.server.feature.reflection.Reflection;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import java.util.stream.Collectors;
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
            ObjectProvider<ServerBuilder<?>> serverBuilder,
            ObjectProvider<BindableService> services,
            ObjectProvider<ServerInterceptor> interceptors,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        return properties.isEnableEmptyServer()
                        || !allInternalServices(services.stream().collect(Collectors.toSet()))
                ? new DefaultGrpcServer(properties, serverBuilder, services, interceptors, customizers)
                : new DummyGrpcServer();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcRequestContextServerInterceptor grpcRequestContextServerInterceptor() {
        return new GrpcRequestContextServerInterceptor();
    }

    @Configuration(proxyBeanMethods = false)
    @Import({Reflection.class, Health.class, Channelz.class, ExceptionHandling.class})
    static class Features {}
}
