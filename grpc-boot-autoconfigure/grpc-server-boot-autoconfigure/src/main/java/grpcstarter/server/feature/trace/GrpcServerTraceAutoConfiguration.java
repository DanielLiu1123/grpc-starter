package grpcstarter.server.feature.trace;

import grpcstarter.server.ConditionOnGrpcServerEnabled;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
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
@ConditionOnGrpcServerEnabled
@ConditionalOnClass(ObservationRegistry.class)
@ConditionalOnBean(ObservationRegistry.class)
public class GrpcServerTraceAutoConfiguration {

    /**
     * A relatively high priority.
     */
    public static final int ORDER = -10000;

    @Bean
    @Order(ORDER)
    @ConditionalOnMissingBean(ObservationGrpcServerInterceptor.class)
    public ObservationGrpcServerInterceptor observationGrpcServerInterceptor(ObservationRegistry registry) {
        return new ObservationGrpcServerInterceptor(registry);
    }
}
