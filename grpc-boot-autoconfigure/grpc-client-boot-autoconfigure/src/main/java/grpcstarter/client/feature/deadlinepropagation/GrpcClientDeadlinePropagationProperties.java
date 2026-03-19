package grpcstarter.client.feature.deadlinepropagation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for gRPC client deadline propagation.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcClientDeadlinePropagationProperties.PREFIX)
public class GrpcClientDeadlinePropagationProperties {
    public static final String PREFIX = "grpc.client.deadline-propagation";

    /**
     * Whether to enable deadline propagation, default is {@code true}.
     */
    private boolean enabled = true;
}
