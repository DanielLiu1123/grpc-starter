package grpcstarter.extensions.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for gRPC security integration.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcSecurityProperties.PREFIX)
public class GrpcSecurityProperties {
    public static final String PREFIX = "grpc.security";

    /**
     * Whether to enable gRPC security, default is {@code true}.
     */
    private boolean enabled = true;

    /**
     * Server-side security configuration.
     */
    private Server server = new Server();

    @Data
    public static class Server {
        public static final String PREFIX = GrpcSecurityProperties.PREFIX + ".server";

        /**
         * Whether to enable server-side security, default is {@code true}.
         */
        private boolean enabled = true;

        /**
         * Order of the {@link GrpcSecurityServerInterceptor}, default is {@code 50}.
         */
        private int order = 50;
    }
}
