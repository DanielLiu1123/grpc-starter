package com.freemanan.starter.grpc.client;

import static java.util.stream.Collectors.toMap;

import io.grpc.health.v1.HealthGrpc;
import io.grpc.internal.GrpcUtil;
import io.grpc.stub.AbstractStub;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
     * Default max message size, default value is {@code 4MB}.
     *
     * @see DataSize
     */
    private DataSize maxMessageSize = DataSize.ofBytes(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE);
    /**
     * Default max metadata size, default value is {@code 8KB}.
     *
     * @see DataSize
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
     * Channel shutdown timeout in milliseconds, default value is {@code 5000}.
     */
    private Long shutdownTimeout = 5000L;
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
         * Max message size for this channel, use {@link GrpcClientProperties#maxMessageSize} if not set.
         */
        private DataSize maxMessageSize;
        /**
         * Max metadata size for this channel, use {@link GrpcClientProperties#maxMetadataSize} if not set.
         */
        private DataSize maxMetadataSize;
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
         * <p> Support Ant-style pattern.
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
         * <p> Support Ant-style pattern.
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
         * <p> If set, will create in-process channel by default, usually for testing.
         */
        private String name;
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

    /**
     * Merge default properties with channel specified properties.
     */
    public void merge() {
        for (Channel stub : channels) {
            if (stub.getAuthority() == null) {
                stub.setAuthority(authority);
            }
            if (stub.getMaxMessageSize() == null) {
                stub.setMaxMessageSize(maxMessageSize);
            }
            if (stub.getMaxMetadataSize() == null) {
                stub.setMaxMetadataSize(maxMetadataSize);
            }
            if (stub.getShutdownTimeout() == null) {
                stub.setShutdownTimeout(shutdownTimeout);
            }
            if (stub.getInProcess() == null) {
                stub.setInProcess(inProcess);
            }
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
                maxMessageSize,
                maxMetadataSize,
                shutdownTimeout,
                metadata,
                inProcess,
                null,
                null,
                null);
    }
}
