package grpcstarter.server;

import static grpcstarter.server.Util.allInternalServices;

import grpcstarter.server.feature.channelz.Channelz;
import grpcstarter.server.feature.exceptionhandling.ExceptionHandling;
import grpcstarter.server.feature.health.Health;
import grpcstarter.server.feature.reflection.Reflection;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.thread.Threading;
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
            SslBundles sslBundles,
            ObjectProvider<ServerBuilder<?>> serverBuilder,
            ObjectProvider<BindableService> services,
            ObjectProvider<ServerInterceptor> interceptors,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        return properties.isEnableEmptyServer()
                        || !allInternalServices(services.stream().collect(Collectors.toSet()))
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
    @ConditionalOnClass({
        ObservationRegistry.class,
        ObservationGrpcServerInterceptor.class,
    })
    static class TraceConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public ObservationGrpcServerInterceptor observationGrpcServerInterceptor(
                Optional<ObservationRegistry> observationRegistry) {
            return new ObservationGrpcServerInterceptor(observationRegistry.orElse(ObservationRegistry.NOOP));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({
        MeterRegistry.class,
        MetricCollectingServerInterceptor.class,
    })
    static class MetricsConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public MetricCollectingServerInterceptor metricCollectingServerInterceptor(
                Optional<MeterRegistry> meterRegistry) {
            return new MetricCollectingServerInterceptor(meterRegistry.orElseGet(CompositeMeterRegistry::new));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({Reflection.class, Health.class, Channelz.class, ExceptionHandling.class})
    static class Features {}
}
