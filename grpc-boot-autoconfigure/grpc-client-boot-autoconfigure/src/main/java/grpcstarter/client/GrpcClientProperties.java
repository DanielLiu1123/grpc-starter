package grpcstarter.client;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;

import io.grpc.Deadline;
import io.grpc.TlsChannelCredentials;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.internal.GrpcUtil;
import io.grpc.stub.AbstractStub;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * gRPC Properties.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(prefix = GrpcClientProperties.PREFIX)
public class GrpcClientProperties implements InitializingBean {
    public static final String PREFIX = "grpc.client";

    /**
     * Whether to enable gRPC client autoconfiguration, default true.
     */
    private boolean enabled = true;
    /**
     * Default authority.
     *
     * <p> e.g. localhost:8080 </p>
     */
    private @Nullable String authority;
    /**
     * Base packages to scan for gRPC stubs.
     *
     * <p> This value will merge with {@link EnableGrpcClients#basePackages}.
     *
     * <p> The advantage of using configuration is no need to introduce external annotations. </p>
     */
    private List<String> basePackages = new ArrayList<>();
    /**
     * gRPC client bean definition handler, used to customize the bean definition before registering.
     *
     * <p> Use {@link GrpcClientBeanDefinitionHandler} if not set.
     *
     * <p> Implementation must have a public no-args constructor.
     *
     * @since 3.4.3.1
     */
    private @Nullable Class<? extends GrpcClientBeanDefinitionHandler> beanDefinitionHandler;
    /**
     * The clients used to register as Spring beans.
     *
     * <p> This value will merge with {@link EnableGrpcClients#clients}.
     *
     * <p> `clients` and {@link #basePackages} represent two different configuration methods.
     * {@link #basePackages} is through package scanning, and `clients` is through class name.
     *
     * <p> The `clients` configuration can avoid classpath scanning,
     * resulting in faster startup, and it has better IDE support.
     */
    @SuppressWarnings("rawtypes")
    private List<Class<? extends AbstractStub>> clients = new ArrayList<>();
    /**
     * Default max inbound message size, default value is 4MB.
     *
     * @see DataSize
     * @see GrpcUtil#DEFAULT_MAX_MESSAGE_SIZE
     */
    private @Nullable DataSize maxInboundMessageSize;
    /**
     * Default max outbound message size.
     *
     * @see DataSize
     * @see AbstractStub#withMaxOutboundMessageSize(int)
     */
    private @Nullable DataSize maxOutboundMessageSize;
    /**
     * Default max metadata size, default value is 8KB.
     *
     * @see DataSize
     * @see GrpcUtil#DEFAULT_MAX_HEADER_LIST_SIZE
     */
    private @Nullable DataSize maxInboundMetadataSize;
    /**
     * Default metadata will be added to all the gRPC requests.
     */
    private List<Metadata> metadata = new ArrayList<>();
    /**
     * In-process client configuration.
     */
    private @Nullable InProcess inProcess;
    /**
     * Channel shutdown timeout in milliseconds, default value is 5000.
     */
    private @Nullable Long shutdownTimeout = 5000L;
    /**
     * TLS configuration.
     *
     * @deprecated Use {@link #sslBundle} instead. This will be removed in 3.6.0
     */
    @Deprecated(since = "3.5.3", forRemoval = true)
    private @Nullable Tls tls;
    /**
     * SSL bundle name to use for secure connections.
     *
     * <p>References an SSL bundle configured under {@code spring.ssl.bundle.*}.
     * This is the preferred way to configure SSL/TLS for gRPC clients.
     *
     * @since 3.5.3
     */
    private @Nullable String sslBundle;
    /**
     * Retry configuration.
     */
    private @Nullable Retry retry;
    /**
     * Deadline after in milliseconds, default value is 5000.
     *
     * @see AbstractStub#withDeadline(Deadline)
     * @since 3.2.0
     */
    private @Nullable Long deadline;
    /**
     * Compression configuration.
     *
     * @see AbstractStub#withCompression(String)
     */
    private @Nullable String compression;
    /**
     * Channels configuration.
     */
    private List<Channel> channels = new ArrayList<>();
    /**
     * Refresh configuration.
     */
    private Refresh refresh = new Refresh();

    @Override
    public void afterPropertiesSet() {
        merge();
        setChannelNames();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        /**
         * Channel name.
         *
         * <p> If not set, will be auto-generated.
         */
        private @Nullable String name;
        /**
         * Authority for this channel, use {@link GrpcClientProperties#authority} if not set.
         */
        private @Nullable String authority;
        /**
         * Max inbound message size, use {@link GrpcClientProperties#maxInboundMessageSize} if not set.
         */
        private @Nullable DataSize maxInboundMessageSize;
        /**
         * Max outbound message size, use {@link GrpcClientProperties#maxOutboundMessageSize} if not set.
         */
        private @Nullable DataSize maxOutboundMessageSize;
        /**
         * Max metadata size for this channel, use {@link GrpcClientProperties#maxInboundMetadataSize} if not set.
         */
        private @Nullable DataSize maxInboundMetadataSize;
        /**
         * Channel shutdown timeout in milliseconds, use {@link GrpcClientProperties#shutdownTimeout} if not set.
         */
        private @Nullable Long shutdownTimeout;
        /**
         * Metadata to be added to the requests for this channel, will be merged with {@link GrpcClientProperties#metadata}.
         */
        private List<Metadata> metadata = new ArrayList<>();
        /**
         * In-process configuration for this channel, use {@link GrpcClientProperties#inProcess} if not set.
         */
        private @Nullable InProcess inProcess;
        /**
         * TLS configuration for this channel, use {@link GrpcClientProperties#tls} if not set.
         *
         * @deprecated Use {@link #sslBundle} instead. This will be removed in 3.6.0
         */
        @Deprecated(since = "3.5.3", forRemoval = true)
        private @Nullable Tls tls;
        /**
         * SSL bundle name to use for secure connections for this channel.
         *
         * <p>References a SSL bundle configured under {@code spring.ssl.bundle.*}.
         * If not set, uses {@link GrpcClientProperties#sslBundle}.
         * This is the preferred way to configure SSL/TLS for gRPC clients.
         *
         * @since 3.5.3
         */
        private @Nullable String sslBundle;
        /**
         * Retry configuration for this channel, use {@link GrpcClientProperties#retry} if not set.
         */
        private @Nullable Retry retry;
        /**
         * Deadline after in milliseconds, use {@link GrpcClientProperties#deadline} if not set.
         *
         * @see AbstractStub#withDeadline(Deadline)
         */
        private @Nullable Long deadline;
        /**
         * Compression for this channel, use {@link GrpcClientProperties#compression} if not set.
         *
         * @see AbstractStub#withCompression(String)
         */
        private @Nullable String compression;
        /**
         * gRPC stub classes to apply this channel.
         *
         * <p> This is a more IDE-friendly alternative to {@link #services}/{@link #stubs}, using classes first if both set. </p>
         *
         * <p> The priority is classes > {@link #stubs} > {@link #services}. </p>
         */
        @SuppressWarnings("rawtypes")
        private List<Class<? extends AbstractStub>> classes = new ArrayList<>();
        /**
         * gRPC stubs to apply this channel.
         *
         * <p> Support Ant-style patterns. </p>
         *
         * <p> e.g. {@link HealthGrpc.HealthBlockingStub} can be identified by </p>
         * <ul>
         *     <li> io.grpc.health.v1.HealthGrpc.HealthBlockingStub (Class canonical name) </li>
         *     <li> io.grpc.health.v1.HealthGrpc$HealthBlockingStub (Class name) </li>
         *     <li> io.grpc.**.*BlockingStub (<a href="https://stackoverflow.com/questions/2952196/ant-path-style-patterns">Ant style pattern</a>) </li>
         * </ul>
         *
         * <p> This is a more flexible alternative to {@link #classes}, using {@link #classes} first if both set. </p>
         *
         * <p> The priority is {@link #classes} > stubs > {@link #services}. </p>
         */
        private List<String> stubs = new ArrayList<>();
        /**
         * gRPC service names to apply this channel.
         *
         * <p> Support Ant-style patterns. </p>
         *
         * <p> e.g. {@link HealthGrpc.HealthBlockingStub} can be identified by </p>
         * <ul>
         *     <li> grpc.health.v1.Health (SERVICE_NAME field in {@link HealthGrpc}) </li>
         *     <li> grpc.health.v*.** (<a href="https://stackoverflow.com/questions/2952196/ant-path-style-patterns">Ant style pattern</a>) </li>
         * </ul>
         *
         * <p> This is a more flexible alternative to {@link #classes}, using {@link #classes} first if both set. </p>
         *
         * <p> The priority is {@link #classes} > {@link #stubs} > services. </p>
         *
         * @see AntPathMatcher
         */
        private List<String> services = new ArrayList<>();
    }

    /**
     * @param key    Header key.
     * @param values Header values.
     */
    public record Metadata(String key, List<String> values) {
        public Metadata {
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("Metadata key must not be empty");
            }
            values = List.copyOf(values);
        }
    }

    /**
     * @param name In-process client name.
     *
     *             <p> If set, will create an in-process channel by default, usually for testing. </p>
     */
    public record InProcess(String name) {
        public InProcess {
            if (!StringUtils.hasText(name)) {
                throw new IllegalArgumentException("In-process name must not be empty");
            }
        }

        public static final String PREFIX = GrpcClientProperties.PREFIX + ".in-process";
    }

    @Data
    public static class Retry {
        public static final String PREFIX = GrpcClientProperties.PREFIX + ".retry";
        /**
         * Whether to enable retry.
         *
         * @see io.grpc.ManagedChannelBuilder#enableRetry()
         * @see io.grpc.ManagedChannelBuilder#disableRetry()
         */
        private @Nullable Boolean enabled;
        /**
         * Maximum number of attempts to retry.
         *
         * @see io.grpc.ManagedChannelBuilder#maxRetryAttempts(int)
         */
        private @Nullable Integer maxRetryAttempts;
        /**
         * The maximum number of retry buffer entries.
         *
         * @see io.grpc.ManagedChannelBuilder#retryBufferSize(long)
         */
        private @Nullable DataSize retryBufferSize;
        /**
         * The maximum number of retry buffer entries per RPC.
         *
         * @see io.grpc.ManagedChannelBuilder#perRpcBufferLimit(long)
         */
        private @Nullable DataSize perRpcBufferLimit;
    }

    @Data
    public static class Refresh {
        public static final String PREFIX = GrpcClientProperties.PREFIX + ".refresh";

        /**
         * Whether to enable refresh grpc clients, default false.
         *
         * <p> NOTE: this feature needs 'spring-cloud-context' dependency in the classpath. </p>
         */
        private boolean enabled = false;
    }

    @Data
    public static class Tls {
        public static final String PREFIX = GrpcClientProperties.PREFIX + ".tls";

        /**
         * @see io.grpc.TlsChannelCredentials.Builder#keyManager(InputStream, InputStream, String)
         * @see io.grpc.TlsChannelCredentials.Builder#keyManager(InputStream, InputStream)
         */
        private @Nullable KeyManager keyManager;
        /**
         * @see io.grpc.TlsChannelCredentials.Builder#trustManager(InputStream)
         */
        private @Nullable TrustManager trustManager;

        @Data
        public static class KeyManager {
            /**
             * @see TlsChannelCredentials.Builder#getCertificateChain()
             */
            private @Nullable Resource certChain;
            /**
             * @see TlsChannelCredentials.Builder#getPrivateKey()
             */
            private @Nullable Resource privateKey;
            /**
             * @see TlsChannelCredentials.Builder#getPrivateKeyPassword()
             */
            private @Nullable String privateKeyPassword;
        }

        @Data
        public static class TrustManager {
            /**
             * @see TlsChannelCredentials.Builder#getRootCertificates()
             */
            private @Nullable Resource rootCerts;
        }
    }

    /**
     * Merge default properties with channel specified properties.
     */
    void merge() {
        PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
        for (Channel stub : channels) {
            mapper.from(maxInboundMessageSize)
                    .when(e -> isNull(stub.getMaxInboundMessageSize()))
                    .to(stub::setMaxInboundMessageSize);
            mapper.from(maxOutboundMessageSize)
                    .when(e -> isNull(stub.getMaxOutboundMessageSize()))
                    .to(stub::setMaxOutboundMessageSize);
            mapper.from(maxInboundMetadataSize)
                    .when(e -> isNull(stub.getMaxInboundMetadataSize()))
                    .to(stub::setMaxInboundMetadataSize);
            mapper.from(shutdownTimeout)
                    .when(e -> isNull(stub.getShutdownTimeout()))
                    .to(stub::setShutdownTimeout);
            mapper.from(inProcess).when(e -> isNull(stub.getInProcess())).to(stub::setInProcess);
            mapper.from(tls).when(e -> isNull(stub.getTls())).to(stub::setTls);
            mapper.from(sslBundle).when(e -> isNull(stub.getSslBundle())).to(stub::setSslBundle);
            mapper.from(retry).when(e -> isNull(stub.getRetry())).to(stub::setRetry);
            mapper.from(deadline).when(e -> isNull(stub.getDeadline())).to(stub::setDeadline);
            mapper.from(compression).when(e -> isNull(stub.getCompression())).to(stub::setCompression);

            // default + client specified
            LinkedHashMap<String, List<String>> total = metadata.stream()
                    .collect(toMap(Metadata::key, Metadata::values, (oldV, newV) -> oldV, LinkedHashMap::new));
            for (Metadata m : stub.getMetadata()) {
                total.put(m.key(), m.values());
            }
            List<Metadata> merged = total.entrySet().stream()
                    .map(e -> new Metadata(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            stub.setMetadata(merged);
        }
    }

    void setChannelNames() {
        var unnamedChannels = channels.stream()
                .filter(ch -> !StringUtils.hasText(ch.getName()))
                .toList();
        for (int i = 0; i < unnamedChannels.size(); i++) {
            unnamedChannels.get(i).setName("unnamed-" + i);
        }

        var names = new HashSet<String>();
        for (var channel : channels) {
            var name = channel.getName();
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (names.contains(name)) {
                throw new IllegalArgumentException("Duplicate channel name: " + name);
            }
            names.add(name);
        }
    }

    Channel defaultChannel() {
        return new Channel(
                "__default__",
                authority,
                maxInboundMessageSize,
                maxOutboundMessageSize,
                maxInboundMetadataSize,
                shutdownTimeout,
                metadata,
                inProcess,
                tls,
                sslBundle,
                retry,
                deadline,
                compression,
                List.of(),
                List.of(),
                List.of());
    }
}
