package grpcstarter.client.feature.metrics;

import grpcstarter.client.ConditionOnGrpcClientEnabled;
import grpcstarter.client.GrpcClientAutoConfiguration;
import grpcstarter.client.GrpcClientProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingClientInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration for gRPC metrics with Micrometer.
 *
 * @author Freeman
 */
@AutoConfiguration(after = CompositeMeterRegistryAutoConfiguration.class)
@ConditionOnGrpcClientEnabled
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
public class GrpcClientMetricsAutoConfiguration {

    /**
     * A relatively high priority.
     */
    public static final int ORDER = -10000;

    @Bean
    @Order(ORDER)
    @ConditionalOnMissingBean(MetricCollectingClientInterceptor.class)
    public MetricCollectingClientInterceptor metricCollectingClientInterceptor(MeterRegistry registry) {
        return new MetricCollectingClientInterceptor(registry);
    }
}
