package com.freemanan.starter.grpc.extensions.test;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcTestProperties.PREFIX)
public class GrpcTestProperties {
    public static final String PREFIX = "grpc.test";

    /**
     * Whether to enable test, default is {@code true}.
     */
    private boolean enabled = true;
    /**
     * Server-side test configuration.
     */
    private Server server = new Server();

    @Data
    public static class Server {
        public static final String PREFIX = GrpcTestProperties.PREFIX + ".server";

        /**
         * Whether to enable test, default is {@code true}.
         */
        private boolean enabled = true;
        /**
         * Port configuration, default is {@link PortType#IN_PROCESS}, which means start grpc server with in-process transport.
         *
         * <p>
         * NOTE: if {@code grpc-client-starter} is not in classpath, will fall back to {@link PortType#RANDOM_PORT}.
         */
        private PortType portType = PortType.IN_PROCESS;
    }

    /**
     * If used as a static internal class of {@link Server}, the IDE's automatic prompt will be lost :)
     */
    public enum PortType {
        /**
         * Start grpc server with in-process transport.
         */
        IN_PROCESS,
        /**
         * Start grpc server with random port.
         */
        RANDOM_PORT,
        /**
         * Start grpc server with defined port.
         *
         * <p> using {@code grpc.server.port} property.
         */
        DEFINED_PORT
    }
}
