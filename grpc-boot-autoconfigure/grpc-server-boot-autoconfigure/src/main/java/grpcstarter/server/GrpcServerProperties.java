package grpcstarter.server;

import grpcstarter.server.feature.exceptionhandling.annotation.DefaultGrpcExceptionAdvice;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.GrpcUtil;
import io.grpc.protobuf.services.ChannelzService;
import io.grpc.services.AdminInterface;
import lombok.Data;
import org.jspecify.annotations.Nullable;
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
     * Whether to enable gRPC server autoconfiguration, default true.
     */
    private boolean enabled = true;
    /**
     * gRPC server port, default 9090, 0 or negative numbers will use random port.
     */
    private int port = 9090;
    /**
     * Graceful shutdown timeout, default 30s, if 0 will wait forever util all active calls finished.
     */
    private long shutdownTimeout = 30000L;
    /**
     * Whether to start a gRPC server when no service found, default true.
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
     * The maximum message size allowed to be received on the server, default 4MB.
     *
     * @see GrpcUtil#DEFAULT_MAX_MESSAGE_SIZE
     */
    @Nullable
    private DataSize maxInboundMessageSize;
    /**
     * The maximum size of metadata allowed to be received, default 8KB.
     *
     * @see GrpcUtil#DEFAULT_MAX_HEADER_LIST_SIZE
     */
    @Nullable
    private DataSize maxInboundMetadataSize;
    /**
     * In-process server configuration.
     */
    @Nullable
    private InProcess inProcess;
    /**
     * SSL bundle name for TLS configuration.
     * <p>
     * This is the preferred way to configure SSL/TLS for gRPC server.
     * When specified, it takes precedence over the deprecated {@link #tls} configuration.
     * </p>
     *
     * @since 3.5.3
     */
    @Nullable
    private String sslBundle;

    /**
     * Response configuration.
     */
    private Response response = new Response();

    @Data
    public static class Reflection {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".reflection";

        /**
         * Whether to register reflection service, default false.
         */
        private boolean enabled = false;
    }

    @Data
    public static class Health {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".health";

        /**
         * Whether to enable health check, default false.
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
             * Whether to enable datasource health check, default true.
             */
            private boolean enabled = true;
            /**
             * The service name that will be used for datasource health check, default value is 'datasource'.
             */
            private String service = "datasource";
            /**
             * The timeout in seconds for {@link java.sql.Connection#isValid(int)}, use 0 if not set.
             */
            private @Nullable Integer timeout;
        }

        @Data
        public static class Redis {
            public static final String PREFIX = Health.PREFIX + ".redis";

            /**
             * Whether to enable redis health check, default true.
             */
            private boolean enabled = true;
            /**
             * The service name that will be used for redis health check, default value is 'redis'.
             */
            private String service = "redis";
        }
    }

    @Data
    public static class Channelz {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".channelz";

        /**
         * Whether to register {@link ChannelzService}, default false.
         */
        private boolean enabled = false;
        /**
         * The maximum page size to return, default 100.
         *
         * @see AdminInterface
         */
        private int maxPageSize = 100;
    }

    @Data
    public static class ExceptionHandling {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".exception-handling";

        /**
         * Whether to enable exception handling, default true.
         */
        private boolean enabled = true;

        /**
         * Whether to enable {@link DefaultGrpcExceptionAdvice}, default true.
         *
         * <p> {@link DefaultGrpcExceptionAdvice} will handle exceptions recognized by gRPC, including: </p>
         * <ul>
         *     <li>{@link StatusRuntimeException}</li>
         *     <li>{@link StatusException}</li>
         * </ul>
         *
         * <p> When enabled, you can directly throw {@link StatusRuntimeException} or {@link StatusException} in service implementation,
         * and the exception will be handled by {@link DefaultGrpcExceptionAdvice}. </p>
         *
         * <pre>{@code
         * @GrpcService
         * public class SimpleService extends SimpleServiceGrpc.SimpleServiceImplBase {
         *     @Override
         *     public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
         *         throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid request"));
         *     }
         * }
         * }</pre>
         *
         * @see DefaultGrpcExceptionAdvice
         * @since 3.2.3
         */
        private boolean defaultExceptionAdviceEnabled = true;
    }

    /**
     * @param name In-process server name, if configured, will create an in-process server, usually for testing.
     */
    public record InProcess(String name) {
        public static final String PREFIX = GrpcServerProperties.PREFIX + ".in-process";
    }

    @Data
    public static class Response {

        /**
         * The maximum length of response description.
         *
         * <p> When the length of the description exceeds this value, it will be truncated. </p>
         *
         * @since 3.2.3
         */
        private Integer maxDescriptionLength = 2048;
    }
}
