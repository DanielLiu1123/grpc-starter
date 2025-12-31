package grpcstarter.server;

import static grpcstarter.server.Util.allInternalServices;

import grpcstarter.server.feature.channelz.Channelz;
import grpcstarter.server.feature.exceptionhandling.ExceptionHandling;
import grpcstarter.server.feature.health.Health;
import grpcstarter.server.feature.reflection.Reflection;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * gRPC Server Auto-configuration.
 *
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
            SslBundles sslBundles,
            Optional<ServerBuilder<?>> serverBuilder,
            List<BindableService> services,
            List<ServerInterceptor> interceptors,
            List<GrpcServerCustomizer> customizers) {
        return (properties.isEnableEmptyServer() || !allInternalServices(services))
                ? new DefaultGrpcServer(properties, sslBundles, serverBuilder, services, interceptors, customizers)
                : new DummyGrpcServer();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcRequestContextServerInterceptor grpcRequestContextServerInterceptor(
            GrpcServerProperties grpcServerProperties) {
        return new GrpcRequestContextServerInterceptor(grpcServerProperties);
    }

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    @ConditionalOnMissingBean
    public VirtualThreadGrpcServerCustomizer virtualThreadGrpcServerCustomizer() {
        return new VirtualThreadGrpcServerCustomizer();
    }

    @Configuration(proxyBeanMethods = false)
    @Import({Reflection.class, Health.class, Channelz.class, ExceptionHandling.class})
    static class Features {}
}
