package com.freemanan.starter.grpc.client;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;

import io.grpc.Deadline;
import io.grpc.TlsChannelCredentials;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.internal.GrpcUtil;
import io.grpc.stub.AbstractStub;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
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
     * Whether to enable gRPC client autoconfiguration, default {@code true}.
     */
    private boolean enabled = true;
    /**
     * Default authority.
     *
     * <p> e.g. {@code localhost:8080}
     */
    private String authority;
    /**
     * Base packages to scan for gRPC stubs.
     *
     * <p> This value will merge with {@link EnableGrpcClients#basePackages}, only takes effect if {@link EnableGrpcClients#basePackages} is not set.
     * <p> The advantage of using configuration is no need to introduce external annotations.
     */
    private List<String> basePackages = new ArrayList<>();
    /**
     * Default max inbound message size, default value is {@code 4MB}.
     *
     * @see DataSize
     * @see GrpcUtil#DEFAULT_MAX_MESSAGE_SIZE
     */
    private DataSize maxInboundMessageSize;
    /**
     * Default max outbound message size.
     *
     * @see DataSize
     * @see AbstractStub#withMaxOutboundMessageSize(int)
     */
    private DataSize maxOutboundMessageSize;
    /**
     * Default max metadata size, default value is {@code 8KB}.
     *
     * @see DataSize
     * @see GrpcUtil#DEFAULT_MAX_HEADER_LIST_SIZE
     */
    private DataSize maxInboundMetadataSize;
    /**
     * Default metadata will be added to all the gRPC requests.
     */
    private List<Metadata> metadata = new ArrayList<>();
    /**
     * In-process client configuration.
     */
    private InProcess inProcess;
    /**
     * Channel shutdown timeout in milliseconds, default value is {@code 5000}.
     */
    private Long shutdownTimeout = 5000L;
    /**
     * TLS configuration.
     */
    private Tls tls;
    /**
     * Retry configuration.
     */
    private Retry retry;
    /**
     * Deadline after in milliseconds, default value is {@code 5000}.
     *
     * @see AbstractStub#withDeadline(Deadline)
     * @since 3.2.0
     */
    private Long deadline;
    /**
     * Compression configuration.
     *
     * @see AbstractStub#withCompression(String)
     */
    private String compression;
    /**
     * Channels configuration.
     */
    private List<Channel> channels = new ArrayList<>();
    /**
     * Refresh configuration.
     */
    private Refresh refresh = new Refresh();
    /**
     * Whether to enable warn unused config, default {@code true}.
     */
    private boolean warnUnusedConfigEnabled = true;

    @Override
    public void afterPropertiesSet() {
        merge();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        /**
         * Channel name, optional.
         */
        private String name;
        /**
         * Authority for this channel, use {@link GrpcClientProperties#authority} if not set.
         */
        private String authority;
        /**
         * Max inbound message size, use {@link GrpcClientProperties#maxInboundMessageSize} if not set.
         */
        private DataSize maxInboundMessageSize;
        /**
         * Max outbound message size, use {@link GrpcClientProperties#maxOutboundMessageSize} if not set.
         */
        private DataSize maxOutboundMessageSize;
        /**
         * Max metadata size for this channel, use {@link GrpcClientProperties#maxInboundMetadataSize} if not set.
         */
        private DataSize maxInboundMetadataSize;
        /**
         * Channel shutdown timeout in milliseconds, use {@link GrpcClientProperties#shutdownTimeout} if not set.
         */
        private Long shutdownTimeout;
        /**
         * Metadata to be added to the requests for this channel, will be merged with {@link GrpcClientProperties#metadata}.
         */
        private List<Metadata> metadata = new ArrayList<>();
        /**
         * In-process configuration for this channel, use {@link GrpcClientProperties#inProcess} if not set.
         */
        private InProcess inProcess;
        /**
         * TLS configuration for this channel, use {@link GrpcClientProperties#tls} if not set.
         */
        private Tls tls;
        /**
         * Retry configuration for this channel, use {@link GrpcClientProperties#retry} if not set.
         */
        private Retry retry;
        /**
         * Deadline after in milliseconds, use {@link GrpcClientProperties#deadline} if not set.
         *
         * @see AbstractStub#withDeadline(Deadline)
         */
        private Long deadline;
        /**
         * Compression for this channel, use {@link GrpcClientProperties#compression} if not set.
         *
         * @see AbstractStub#withCompression(String)
         */
        private String compression;
        /**
         * gRPC stub classes to apply this channel.
         *
         * <p> This is a more IDE-friendly alternative to {@link #services}/{@link #stubs}, using classes first if both set.
         *
         * <p> The priority is classes > {@link #stubs} > {@link #services}.
         */
        @SuppressWarnings("rawtypes")
        private List<Class<? extends AbstractStub>> classes = new ArrayList<>();
        /**
         * gRPC stubs to apply this channel.
         *
         * <p> Support Ant-style patterns.
         *
         * <p> e.g. {@link HealthGrpc.HealthBlockingStub} can be identified by
         * <ul>
         *     <li> {@code io.grpc.health.v1.HealthGrpc.HealthBlockingStub} (Class canonical name) </li>
         *     <li> {@code io.grpc.health.v1.HealthGrpc$HealthBlockingStub} (Class name) </li>
         *     <li> {@code io.grpc.**.*BlockingStub} (<a href="https://stackoverflow.com/questions/2952196/ant-path-style-patterns">Ant style pattern</a>) </li>
         * </ul>
         *
         * <p> This is a more flexible alternative to {@link #classes}, using {@link #classes} first if both set.
         *
         * <p> The priority is {@link #classes} > stubs > {@link #services}.
         */
        private List<String> stubs = new ArrayList<>();
        /**
         * gRPC service names to apply this channel.
         *
         * <p> Support Ant-style patterns.
         *
         * <p> e.g. {@link HealthGrpc.HealthBlockingStub} can be identified by
         * <ul>
         *     <li> {@code grpc.health.v1.Health} (SERVICE_NAME field in {@link HealthGrpc}) </li>
         *     <li> {@code grpc.health.v*.**} (<a href="https://stackoverflow.com/questions/2952196/ant-path-style-patterns">Ant style pattern</a>) </li>
         * </ul>
         *
         * <p> This is a more flexible alternative to {@link #classes}, using {@link #classes} first if both set.
         *
         * <p> The priority is {@link #classes} > {@link #stubs} > services.
         *
         * @see AntPathMatcher
         */
        private List<String> services = new ArrayList<>();
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
    public static class InProcess {
        public static final String PREFIX = GrpcClientProperties.PREFIX + ".in-process";

        /**
         * In-process client name.
         *
         * <p> If set, will create an in-process channel by default, usually for testing.
         */
        private String name;
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
        private Boolean enabled;
        /**
         * Maximum number of attempts to retry.
         *
         * @see io.grpc.ManagedChannelBuilder#maxRetryAttempts(int)
         */
        private Integer maxRetryAttempts;
        /**
         * The maximum number of retry buffer entries.
         *
         * @see io.grpc.ManagedChannelBuilder#retryBufferSize(long)
         */
        private DataSize retryBufferSize;
        /**
         * The maximum number of retry buffer entries per RPC.
         *
         * @see io.grpc.ManagedChannelBuilder#perRpcBufferLimit(long)
         */
        private DataSize perRpcBufferLimit;
    }

    @Data
    public static class Refresh {
        public static final String PREFIX = GrpcClientProperties.PREFIX + ".refresh";

        /**
         * Whether to enable refresh grpc clients, default {@code false}.
         *
         * <p> NOTE: this feature needs {@code spring-cloud-context} dependency in the classpath.
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
        private KeyManager keyManager;
        /**
         * @see io.grpc.TlsChannelCredentials.Builder#trustManager(InputStream)
         */
        private TrustManager trustManager;

        @Data
        public static class KeyManager {
            /**
             * @see TlsChannelCredentials.Builder#getCertificateChain()
             */
            private Resource certChain;
            /**
             * @see TlsChannelCredentials.Builder#getPrivateKey()
             */
            private Resource privateKey;
            /**
             * @see TlsChannelCredentials.Builder#getPrivateKeyPassword()
             */
            private String privateKeyPassword;
        }

        @Data
        public static class TrustManager {
            /**
             * @see TlsChannelCredentials.Builder#getRootCertificates()
             */
            private Resource rootCerts;
        }
    }

    /**
     * Merge default properties with channel specified properties.
     */
    public void merge() {
        for (Channel stub : channels) {
            PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();

            mapper.from(authority).when(e -> isNull(stub.getAuthority())).to(stub::setAuthority);
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
            mapper.from(retry).when(e -> isNull(stub.getRetry())).to(stub::setRetry);
            mapper.from(deadline).when(e -> isNull(stub.getDeadline())).to(stub::setDeadline);
            mapper.from(compression).when(e -> isNull(stub.getCompression())).to(stub::setCompression);

            // default + client specified
            LinkedHashMap<String, List<String>> total = metadata.stream()
                    .collect(toMap(Metadata::getKey, Metadata::getValues, (oldV, newV) -> oldV, LinkedHashMap::new));
            for (Metadata m : stub.getMetadata()) {
                total.put(m.getKey(), m.getValues());
            }
            List<Metadata> merged = total.entrySet().stream()
                    .map(e -> new Metadata(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            stub.setMetadata(merged);
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
                retry,
                deadline,
                compression,
                null,
                null,
                null);
    }
}
