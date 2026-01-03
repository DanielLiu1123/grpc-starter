package grpcstarter.client.feature.trace;

import grpcstarter.client.ConditionOnGrpcClientEnabled;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration for gRPC tracing with Micrometer Observation.
 *
 * @author Freeman
 */
@AutoConfiguration(after = ObservationAutoConfiguration.class)
@ConditionOnGrpcClientEnabled
@ConditionalOnClass(ObservationRegistry.class)
@ConditionalOnBean(ObservationRegistry.class)
public class GrpcClientTraceAutoConfiguration {

    /**
     * A relatively high priority.
     */
    public static final int ORDER = -10000;

    @Bean
    @Order(ORDER)
    @ConditionalOnMissingBean(ObservationGrpcClientInterceptor.class)
    public ObservationGrpcClientInterceptor observationGrpcClientInterceptor(ObservationRegistry registry) {
        return new ObservationGrpcClientInterceptor(registry);
    }
}
