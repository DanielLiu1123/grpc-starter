package grpcstarter.extensions.tracing;

import grpcstarter.client.ConditionOnGrpcClientEnabled;
import grpcstarter.client.GrpcClientProperties;
import grpcstarter.server.ConditionOnGrpcServerEnabled;
import grpcstarter.server.GrpcServerProperties;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@AutoConfiguration(after = ObservationAutoConfiguration.class)
@ConditionalOnClass(Tracer.class)
@ConditionalOnProperty(prefix = GrpcTracingProperties.PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnBean(ObservationRegistry.class)
@EnableConfigurationProperties(GrpcTracingProperties.class)
public class GrpcTracingAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ObservationGrpcServerInterceptor.class, GrpcServerProperties.class})
    @ConditionalOnProperty(prefix = GrpcTracingProperties.Server.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionOnGrpcServerEnabled
    static class Server {

        @Bean
        @ConditionalOnMissingBean(ObservationGrpcServerInterceptor.class)
        public OrderedObservationGrpcServerInterceptor orderedObservationGrpcServerInterceptor(
                ObservationRegistry registry, GrpcTracingProperties properties) {
            return new OrderedObservationGrpcServerInterceptor(
                    registry, properties.getServer().getOrder());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ObservationGrpcClientInterceptor.class, GrpcClientProperties.class})
    @ConditionalOnProperty(prefix = GrpcTracingProperties.Client.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionOnGrpcClientEnabled
    static class Client {

        @Bean
        @ConditionalOnMissingBean(ObservationGrpcClientInterceptor.class)
        public OrderedObservationGrpcClientInterceptor orderedObservationGrpcClientInterceptor(
                ObservationRegistry registry, GrpcTracingProperties properties) {
            return new OrderedObservationGrpcClientInterceptor(
                    registry, properties.getClient().getOrder());
        }
    }
}
