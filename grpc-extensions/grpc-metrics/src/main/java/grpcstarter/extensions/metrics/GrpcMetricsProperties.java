package grpcstarter.extensions.metrics;

import static grpcstarter.extensions.metrics.GrpcMetricsProperties.PREFIX;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * gRPC metrics properties.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(PREFIX)
public class GrpcMetricsProperties {
    public static final String PREFIX = "grpc.metrics";

    /**
     * whether to enable metrics, default is {@code true}
     */
    private boolean enabled = true;
    /**
     * Server metrics configuration.
     */
    private Server server = new Server();
    /**
     * Client metrics configuration.
     */
    private Client client = new Client();

    @Data
    public static class Server {
        public static final String PREFIX = GrpcMetricsProperties.PREFIX + ".server";
        /**
         * whether to enable server metrics, default is {@code true}
         */
        private boolean enabled = true;
        /**
         * The order of the server metrics interceptor. Default is {@code 0}.
         */
        private int order = 0;
    }

    @Data
    public static class Client {
        public static final String PREFIX = GrpcMetricsProperties.PREFIX + ".client";
        /**
         * whether to enable client metrics, default is {@code true}
         */
        private boolean enabled = true;
        /**
         * The order of the client metrics interceptor. Default is {@code 0}.
         */
        private int order = 0;
    }
}
