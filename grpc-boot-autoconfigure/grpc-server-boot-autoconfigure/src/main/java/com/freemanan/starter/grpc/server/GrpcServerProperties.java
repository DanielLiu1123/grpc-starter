package com.freemanan.starter.grpc.server;

import io.grpc.internal.GrpcUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * gRPC server Properties.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(prefix = GrpcServerProperties.PREFIX)
public class GrpcServerProperties {
    public static final String PREFIX = "grpc.server";

    /**
     * Whether to enable gRPC server autoconfiguration, default {@code true}.
     */
    private boolean enabled = true;
    /**
     * gRPC server port, default {@code 9090}, {@code 0} or negative number will use random port.
     */
    private int port = 9090;
    /**
     * Graceful shutdown timeout, default {@code 5s}, if {@code 0} will wait forever util all active calls finished.
     */
    private long shutdownTimeout = 5000L;
    /**
     * Debug configuration.
     */
    private Debug debug = new Debug();
    /**
     * Health check configuration.
     */
    private HealthCheck healthCheck = new HealthCheck();
    /**
     * Exception handling configuration.
     */
    private ExceptionHandling exceptionHandling = new ExceptionHandling();
    /**
     * The maximum message size allowed to be received on the server, default {@code 4MB}.
     */
    private DataSize maxMessageSize = DataSize.ofBytes(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE);
    /**
     * The maximum size of metadata allowed to be received, default {@code 8KB}.
     */
    private DataSize maxMetadataSize = DataSize.ofBytes(GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE);
    /**
     * In-process server configuration.
     */
    private InProcess inProcess;

    @Data
    public static class Debug {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".debug";

        /**
         * Whether to enable debug mode (register reflection service), default {@code false}.
         */
        private boolean enabled = false;
    }

    @Data
    public static class HealthCheck {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".health-check";

        /**
         * Whether to enable health check, default {@code false}
         */
        private boolean enabled = true;

        /**
         * Data source health check configuration.
         */
        private DataSource datasource = new DataSource();

        /**
         * Redis health check configuration.
         */
        private Redis redis = new Redis();

        @Data
        public static class DataSource {
            public static final String PREFIX = HealthCheck.PREFIX + ".datasource";

            /**
             * Whether to enable datasource health check, default {@code true}.
             */
            private boolean enabled = true;
            /**
             * The SQL query that will be used to validate datasource connection, default {@code SELECT 1}.
             */
            private String validationQuery = "SELECT 1";
            /**
             * {@link #validationQuery} timeout, unit seconds, default {@code 2} seconds.
             */
            private int timeout = 2;
        }

        @Data
        public static class Redis {
            public static final String PREFIX = HealthCheck.PREFIX + ".redis";

            /**
             * Whether to enable redis health check, default {@code true}.
             */
            private boolean enabled = true;
        }
    }

    @Data
    public static class ExceptionHandling {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".exception-handling";

        /**
         * Whether to enable exception handling, default {@code true}
         */
        private boolean enabled = true;
        /**
         * Whether to enable default exception handler, default {@code false}
         *
         * <p> Default exception handler is {@link com.freemanan.starter.grpc.server.feature.exceptionhandling.DefaultExceptionHandler}.
         */
        private boolean useDefault = false;
    }

    @Data
    public static class InProcess {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".in-process";

        /**
         * In-process server name, if configured, will create in-process server, usually for testing.
         */
        private String name;
    }
}
