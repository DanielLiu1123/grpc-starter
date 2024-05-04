package grpcstarter.extensions.transcoding;

import java.util.UUID;
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
     * In-process name for gRPC transcoding server, default is a random UUID.
     */
    private String inProcessName = UUID.randomUUID().toString();
}
