package com.freemanan.starter.grpc.extensions.discovery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcDiscoveryProperties.PREFIX)
public class GrpcDiscoveryProperties {
    public static final String PREFIX = "grpc.discovery";

    /**
     * Whether to enable discovery, default is {@code true}.
     */
    private boolean enabled = true;

    private Server server = new Server();

    private Client client = new Client();

    @Data
    public static class Server {
        public static final String PREFIX = GrpcDiscoveryProperties.PREFIX + ".server";
        /**
         * Whether to enable discovery registration, default is {@code true}.
         */
        private boolean enabled = true;
    }

    @Data
    public static class Client {
        public static final String PREFIX = GrpcDiscoveryProperties.PREFIX + ".client";
        /**
         * Whether to enable client-side load balancing, default is {@code true}.
         */
        private boolean enabled = true;
    }
}
