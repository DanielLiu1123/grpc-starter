package grpcstarter.server;

import static grpcstarter.server.Util.allInternalServices;

import grpcstarter.server.feature.channelz.Channelz;
import grpcstarter.server.feature.exceptionhandling.ExceptionHandling;
import grpcstarter.server.feature.health.Health;
import grpcstarter.server.feature.reflection.Reflection;
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
    //    @ConditionalOnMissingBean
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
    public GrpcRequestContextServerInterceptor grpcRequestContextServerInterceptor(
            GrpcServerProperties grpcServerProperties) {
        return new GrpcRequestContextServerInterceptor(grpcServerProperties);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({Reflection.class, Health.class, Channelz.class, ExceptionHandling.class})
    static class Features {}
}
