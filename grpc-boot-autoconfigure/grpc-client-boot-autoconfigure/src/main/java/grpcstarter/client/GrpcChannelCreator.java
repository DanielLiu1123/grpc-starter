package grpcstarter.client;

import static grpcstarter.client.GrpcClientUtil.getRefresh;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.TlsChannelCredentials;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * @author Freeman
 */
class GrpcChannelCreator {

    private final BeanFactory beanFactory;
    private final GrpcClientProperties.Refresh refreshConfig;
    private final GrpcClientProperties.Channel channelConfig;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    GrpcChannelCreator(BeanFactory beanFactory, GrpcClientProperties.Channel channelConfig) {
        this.beanFactory = beanFactory;
        this.refreshConfig = getRefresh(beanFactory.getBean(Environment.class));
        this.channelConfig = channelConfig;
    }

    public ManagedChannel create() {
        // One channel configuration results in the creation of one gRPC channel.
        // See https://github.com/DanielLiu1123/grpc-starter/issues/23
        var newestConfig = getLatestChannelConfig();
        return Cache.getOrSupplyChannel(newestConfig, () -> buildChannel(newestConfig));
    }

    private GrpcClientProperties.Channel getLatestChannelConfig() {
        if (refreshConfig.isEnabled()) {
            // NOTE: If we support refresh, we need to get the latest config,
            // because the properties may have been updated at runtime.
            var properties = beanFactory.getBean(GrpcClientProperties.class);
            String channelName = channelConfig.getName();
            if (channelName == null) {
                return channelConfig;
            }
            var channel = Util.findChannelByName(channelName, properties);
            if (channel != null) {
                return channel;
            }

            // Channel name changed, just return the old one
            return channelConfig;
        }
        return channelConfig;
    }

    private ManagedChannel buildChannel(GrpcClientProperties.Channel channelConfig) {
        ManagedChannelBuilder<?> builder = getManagedChannelBuilder(channelConfig);

        // set max message size and max metadata size
        Optional.ofNullable(channelConfig.getMaxInboundMessageSize())
                .map(DataSize::toBytes)
                .map(Long::intValue)
                .ifPresent(builder::maxInboundMessageSize);
        Optional.ofNullable(channelConfig.getMaxInboundMetadataSize())
                .map(DataSize::toBytes)
                .map(Long::intValue)
                .ifPresent(builder::maxInboundMetadataSize);

        setRetry(channelConfig, builder);

        // add default metadata
        Metadata metadata = new Metadata();
        channelConfig.getMetadata().forEach(m -> {
            Metadata.Key<String> key = Metadata.Key.of(m.key(), Metadata.ASCII_STRING_MARSHALLER);
            m.values().forEach(v -> metadata.put(key, v));
        });
        if (!metadata.keys().isEmpty()) {
            ClientInterceptor metadataInterceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
            builder.intercept(metadataInterceptor);
        }

        // set interceptors, gRPC invoke interceptors in reverse order
        var interceptors = beanFactory
                .getBeanProvider(ClientInterceptor.class)
                .orderedStream()
                .toList();
        for (var i = interceptors.size() - 1; i >= 0; i--) {
            builder.intercept(interceptors.get(i));
        }

        // apply customizers
        beanFactory
                .getBeanProvider(GrpcChannelCustomizer.class)
                .orderedStream()
                .forEach(cc -> cc.customize(channelConfig, builder));

        return builder.build();
    }

    private static void setRetry(GrpcClientProperties.Channel channelConfig, ManagedChannelBuilder<?> builder) {
        GrpcClientProperties.Retry retry = channelConfig.getRetry();
        if (retry != null) {
            Optional.ofNullable(retry.getEnabled()).ifPresent(enabled -> {
                if (enabled) {
                    builder.enableRetry();
                } else {
                    builder.disableRetry();
                }
            });
            Optional.ofNullable(retry.getMaxRetryAttempts()).ifPresent(builder::maxRetryAttempts);
            Optional.ofNullable(retry.getRetryBufferSize())
                    .map(DataSize::toBytes)
                    .ifPresent(builder::retryBufferSize);
            Optional.ofNullable(retry.getPerRpcBufferLimit())
                    .map(DataSize::toBytes)
                    .ifPresent(builder::perRpcBufferLimit);
        }
    }

    @SneakyThrows
    private ManagedChannelBuilder<?> getManagedChannelBuilder(GrpcClientProperties.Channel channelConfig) {
        if (channelConfig.getInProcess() != null) {
            var name = channelConfig.getInProcess().name();
            if (!StringUtils.hasText(name)) {
                throw new IllegalArgumentException("In-process name must not be empty");
            }
            return InProcessChannelBuilder.forName(name).directExecutor();
        }

        // Priority: SSL Bundle > Plain text
        String sslBundleName = channelConfig.getSslBundle();

        String authority = channelConfig.getAuthority();
        if (authority == null) {
            throw new IllegalArgumentException("Channel authority cannot be null");
        }

        if (StringUtils.hasText(sslBundleName)) {
            return createChannelWithSslBundle(authority, sslBundleName);
        } else {
            return Grpc.newChannelBuilder(authority, InsecureChannelCredentials.create());
        }
    }

    private ManagedChannelBuilder<?> createChannelWithSslBundle(String authority, String sslBundleName) {
        SslBundles sslBundles = beanFactory.getBean(SslBundles.class);
        SslBundle sslBundle = sslBundles.getBundle(sslBundleName);

        TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder();

        // Set key managers if available
        var keyManagers = sslBundle.getManagers().getKeyManagers();
        if (keyManagers != null && keyManagers.length > 0) {
            tlsBuilder.keyManager(keyManagers);
        }

        // Set trust managers if available
        var trustManagers = sslBundle.getManagers().getTrustManagers();
        if (trustManagers != null && trustManagers.length > 0) {
            tlsBuilder.trustManager(trustManagers);
        }

        return Grpc.newChannelBuilder(authority, tlsBuilder.build());
    }
}
