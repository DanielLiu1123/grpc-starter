package com.freemanan.starter.grpc.server;

import io.grpc.internal.GrpcUtil;
import io.grpc.protobuf.services.ChannelzService;
import io.grpc.services.AdminInterface;
import java.io.InputStream;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
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
     * Whether to start a gRPC server when no service found, default {@code true}.
     */
    private boolean enableEmptyServer = true;
    /**
     * Reflection configuration.
     */
    private Reflection reflection = new Reflection();
    /**
     * Health configuration.
     */
    private Health health = new Health();
    /**
     * Channelz configuration.
     */
    private Channelz channelz = new Channelz();
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
    /**
     * TLS configuration.
     */
    private Tls tls;

    @Data
    public static class Reflection {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".reflection";

        /**
         * Whether to register reflection service, default {@code false}.
         */
        private boolean enabled = false;
    }

    @Data
    public static class Health {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".health";

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
            public static final String PREFIX = Health.PREFIX + ".datasource";

            /**
             * Whether to enable datasource health check, default {@code true}.
             */
            private boolean enabled = true;
            /**
             * The service name that will be used for datasource health check, default {@code datasource}.
             */
            private String service = "datasource";
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
            public static final String PREFIX = Health.PREFIX + ".redis";

            /**
             * Whether to enable redis health check, default {@code true}.
             */
            private boolean enabled = true;
            /**
             * The service name that will be used for redis health check, default {@code redis}.
             */
            private String service = "redis";
        }
    }

    @Data
    public static class Channelz {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".channelz";

        /**
         * Whether to register {@link ChannelzService}, default {@code false}.
         */
        private boolean enabled = false;
        /**
         * The maximum page size to return, default {@code 100}
         *
         * @see AdminInterface#DEFAULT_CHANNELZ_MAX_PAGE_SIZE
         */
        private int maxPageSize = 100;
    }

    @Data
    public static class ExceptionHandling {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".exception-handling";

        /**
         * Whether to enable exception handling, default {@code true}
         */
        private boolean enabled = true;
    }

    @Data
    public static class InProcess {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".in-process";

        /**
         * In-process server name, if configured, will create an in-process server, usually for testing.
         */
        private String name;
    }

    @Data
    public static class Tls {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".tls";

        /**
         * @see io.grpc.TlsServerCredentials.Builder#keyManager(InputStream, InputStream, String)
         * @see io.grpc.TlsServerCredentials.Builder#keyManager(InputStream, InputStream)
         */
        private KeyManager keyManager;
        /**
         * @see io.grpc.TlsServerCredentials.Builder#trustManager(InputStream)
         */
        private TrustManager trustManager;

        @Data
        public static class KeyManager {
            /**
             * @see io.grpc.TlsServerCredentials.Builder#certificateChain
             */
            private Resource certChain;
            /**
             * @see io.grpc.TlsServerCredentials.Builder#privateKey
             */
            private Resource privateKey;
            /**
             * @see io.grpc.TlsServerCredentials.Builder#privateKeyPassword
             */
            private String privateKeyPassword;
        }

        @Data
        public static class TrustManager {
            /**
             * @see io.grpc.TlsServerCredentials.Builder#rootCertificates
             */
            private Resource rootCerts;
        }
    }
}
