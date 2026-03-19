package grpcstarter.client.feature.deadlinepropagation;

import grpcstarter.client.ConditionOnGrpcClientEnabled;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for gRPC client deadline propagation.
 *
 * @author Freeman
 */
@AutoConfiguration
@ConditionOnGrpcClientEnabled
@ConditionalOnProperty(prefix = GrpcClientDeadlinePropagationProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcClientDeadlinePropagationProperties.class)
public class GrpcClientDeadlinePropagationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DeadlinePropagationClientInterceptor deadlinePropagationClientInterceptor() {
        return new DeadlinePropagationClientInterceptor();
    }
}
