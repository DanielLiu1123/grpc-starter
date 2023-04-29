package com.freemanan.starter.grpc;

import static java.util.stream.Collectors.toMap;

import io.grpc.internal.GrpcUtil;
import io.grpc.stub.AbstractStub;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * gRPC Properties.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(prefix = GrpcProperties.PREFIX)
public class GrpcProperties implements InitializingBean {
    public static final String PREFIX = "grpc";

    /**
     * Whether to enable gRPC autoconfiguration, default {@code true}.
     */
    private boolean enabled = true;
    /**
     * gRPC client configuration.
     */
    private Client client = new Client();
    /**
     * gRPC server configuration.
     */
    private Server server = new Server();

    @Override
    public void afterPropertiesSet() throws Exception {
        client.mergeConfig();
    }

    @Data
    public static class Client {
        public static final String PREFIX = GrpcProperties.PREFIX + ".client";

        /**
         * Whether to enable gRPC client autoconfiguration, default {@code true}.
         */
        private boolean enabled = true;
        /**
         * Validation configuration.
         */
        private Validation validation = new Validation();
        /**
         * Default authority.
         *
         * <p> e.g. {@code localhost:8080}
         */
        private String authority;
        /**
         * Default max message size, default value is {@code 4MB}.
         */
        private DataSize maxMessageSize = DataSize.ofBytes(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE);
        /**
         * Default max metadata size, default value is {@code 8KB}.
         */
        private DataSize maxMetadataSize = DataSize.ofBytes(GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE);
        /**
         * Default metadata, will be added to all the gRPC requests.
         */
        private List<Metadata> metadata = new ArrayList<>();
        /**
         * In-process client configuration.
         */
        private InProcess inProcess;
        /**
         * gRPC stub configurations.
         */
        private List<Stub> stubs = new ArrayList<>();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Stub {
            /**
             * gRPC client service name.
             */
            private String service;
            /**
             * gRPC client class.
             *
             * <p> This a more IDE friendly way to identify a client.
             *
             * <p> Properties {@link #service} and stubClass are used to identify a gRPC client, use stubClass first if both set.
             */
            @SuppressWarnings("rawtypes")
            private Class<? extends AbstractStub> stubClass;
            /**
             * gRPC channel authority, use {@link Client#authority} if not set.
             */
            private String authority;
            /**
             * Max message size, use {@link Client#maxMessageSize} if not set.
             */
            private DataSize maxMessageSize;
            /**
             * Max metadata size, use {@link Client#maxMetadataSize} if not set.
             */
            private DataSize maxMetadataSize;
            /**
             * Metadata to be added to the requests, use {@link Client#metadata} if not set.
             */
            private List<Metadata> metadata = new ArrayList<>();
            /**
             * In-process configuration for this stub, use {@link Client#inProcess} if not set.
             */
            private InProcess inProcess;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Metadata {
            /**
             * Header key.
             */
            private String key;
            /**
             * Header values.
             */
            private List<String> values = new ArrayList<>();
        }

        @Data
        public static class Validation {
            public static final String PREFIX = Client.PREFIX + ".validation";
            /**
             * Whether to enable validation, default {@code true}.
             */
            private boolean enabled = true;
        }

        @Data
        public static class InProcess {
            /**
             * In-process client name.
             *
             * <p> If set, will create in-process channel by default, usually for testing.
             */
            private String name;
        }

        public void mergeConfig() {
            for (Stub stub : stubs) {
                if (stub.getAuthority() == null) {
                    stub.setAuthority(authority);
                }
                if (stub.getMaxMessageSize() == null) {
                    stub.setMaxMessageSize(maxMessageSize);
                }
                if (stub.getMaxMetadataSize() == null) {
                    stub.setMaxMetadataSize(maxMetadataSize);
                }
                if (stub.getInProcess() == null) {
                    stub.setInProcess(inProcess);
                }
                // default + client specified
                LinkedHashMap<String, List<String>> total = metadata.stream()
                        .collect(
                                toMap(Metadata::getKey, Metadata::getValues, (oldV, newV) -> oldV, LinkedHashMap::new));
                for (Metadata m : stub.getMetadata()) {
                    total.put(m.getKey(), m.getValues());
                }
                List<Metadata> merged = total.entrySet().stream()
                        .map(e -> new Metadata(e.getKey(), e.getValue()))
                        .toList();
                stub.setMetadata(merged);
            }
        }

        public Stub defaultClient() {
            return new Stub(null, null, authority, maxMessageSize, maxMetadataSize, metadata, null);
        }
    }

    @Data
    public static class Server {
        public static final String PREFIX = GrpcProperties.PREFIX + ".server";

        /**
         * Whether to enable gRPC server autoconfiguration, default {@code true}.
         */
        private boolean enabled = true;
        /**
         * gRPC server port, default {@code 9090}, if {@code 0} will use a random port.
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
         * Validation configuration.
         */
        private Validation validation = new Validation();
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
            public static final String PREFIX = Server.PREFIX + ".debug";

            /**
             * Whether to enable debug mode (register reflection service), default {@code false}.
             */
            private boolean enabled = false;
        }

        @Data
        public static class Validation {
            public static final String PREFIX = Server.PREFIX + ".validation";

            /**
             * Whether to enable validation, default {@code false}
             */
            private boolean enabled = true;
        }

        @Data
        public static class HealthCheck {
            public static final String PREFIX = Server.PREFIX + ".health-check";

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
            public static final String PREFIX = Server.PREFIX + ".exception-handling";

            /**
             * Whether to enable exception handler, default {@code true}
             */
            private boolean enabled = true;
        }

        @Data
        public static class InProcess {
            /**
             * In-process server name, if configured, will create in-process server, usually for testing.
             */
            private String name;
        }
    }
}
