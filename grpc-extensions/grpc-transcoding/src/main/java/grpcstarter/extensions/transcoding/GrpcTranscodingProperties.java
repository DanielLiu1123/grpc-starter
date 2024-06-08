package grpcstarter.extensions.transcoding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcTranscodingProperties.PREFIX)
public class GrpcTranscodingProperties {
    public static final String PREFIX = "grpc.transcoding";

    /**
     * Whether to enable transcoding auto-configuration, default {@code true}.
     */
    private boolean enabled = true;

    /**
     * gRPC server endpoint, if not set, will use {@code localhost:${grpc.server.port}}.
     */
    private String endpoint;
}
