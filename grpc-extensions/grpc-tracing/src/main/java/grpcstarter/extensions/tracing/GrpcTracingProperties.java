package grpcstarter.extensions.tracing;

import static grpcstarter.extensions.tracing.GrpcTracingProperties.PREFIX;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * gRPC tracing properties.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(PREFIX)
public class GrpcTracingProperties {
    public static final String PREFIX = "grpc.tracing";

    /**
     * whether to enable tracing, default is {@code true}
     */
    private boolean enabled = true;
    /**
     * Server tracing configuration.
     */
    private Server server = new Server();
    /**
     * Client tracing configuration.
     */
    private Client client = new Client();

    @Data
    public static class Server {
        public static final String PREFIX = GrpcTracingProperties.PREFIX + ".server";
        /**
         * whether to enable server tracing, default is {@code true}
         */
        private boolean enabled = true;
        /**
         * The order of the server tracing interceptor. Default is {@code 0}.
         */
        private int order = 0;
    }

    @Data
    public static class Client {
        public static final String PREFIX = GrpcTracingProperties.PREFIX + ".client";
        /**
         * whether to enable client tracing, default is {@code true}
         */
        private boolean enabled = true;
        /**
         * The order of the client tracing interceptor. Default is {@code 0}.
         */
        private int order = 0;
    }
}
