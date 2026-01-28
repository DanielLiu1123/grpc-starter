package grpcstarter.server.feature.metrics;

import grpcstarter.server.ConditionOnGrpcServerEnabled;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor;
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
@ConditionOnGrpcServerEnabled
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
public class GrpcServerMetricsAutoConfiguration {

    /**
     * A relatively high priority.
     */
    public static final int ORDER = -10000;

    @Bean
    @Order(ORDER)
    @ConditionalOnMissingBean(MetricCollectingServerInterceptor.class)
    public MetricCollectingServerInterceptor metricCollectingServerInterceptor(MeterRegistry registry) {
        return new MetricCollectingServerInterceptor(registry);
    }
}
