package com.freemanan.starter.grpc.client;

import static java.util.stream.Collectors.toMap;

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

    @Override
    public void afterPropertiesSet() {
        merge();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        // TODO(Freeman): channel graceful shutdown?
        /**
         * Authority for this channel.
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
         * gRPC service names to apply this channel.
         *
         * <p> TODO(Freeman): support wildcard, e.g. {@code pet.v*.*Service}
         */
        private List<String> services = new ArrayList<>();
        /**
         * gRPC stub classes to apply this channel.
         *
         * <p> This is a more IDE friendly way to identify gRPC stubs.
         * <p> Properties {@link #services} and stubs are used to identify gRPC stubs, use stubs first if both set.
         */
        @SuppressWarnings("rawtypes")
        private List<Class<? extends AbstractStub>> stubs = new ArrayList<>();
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
                authority, maxMessageSize, maxMetadataSize, shutdownTimeout, metadata, inProcess, null, null);
    }
}
