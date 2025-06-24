package grpcstarter.client;

import grpcstarter.client.exception.MissingChannelConfigurationException;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.TlsChannelCredentials;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import java.util.Optional;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * @author Freeman
 */
class GrpcChannelCreator {

    private static final Logger log = LoggerFactory.getLogger(GrpcChannelCreator.class);

    private final BeanFactory beanFactory;
    private final Class<?> stubClass;

    GrpcChannelCreator(BeanFactory beanFactory, Class<?> stubClass) {
        this.beanFactory = beanFactory;
        this.stubClass = stubClass;
    }

    public ManagedChannel create() {
        GrpcClientProperties grpcClientProperties = beanFactory.getBean(
                GrpcClientProperties.class); // get from beanFactory first because it can be refreshed

        GrpcClientProperties.Channel channelConfig = getMatchedConfig(stubClass, grpcClientProperties);

        // One channel configuration results in the creation of one gRPC channel.
        // See https://github.com/DanielLiu1123/grpc-starter/issues/23
        return Cache.getOrSupplyChannel(channelConfig, () -> buildChannel(channelConfig));
    }

    static GrpcClientProperties.Channel getMatchedConfig(
            Class<?> stubClass, GrpcClientProperties grpcClientProperties) {
        return Util.findMatchedConfig(stubClass, grpcClientProperties).orElseGet(grpcClientProperties::defaultChannel);
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
            Metadata.Key<String> key = Metadata.Key.of(m.getKey(), Metadata.ASCII_STRING_MARSHALLER);
            m.getValues().forEach(v -> metadata.put(key, v));
        });
        if (!metadata.keys().isEmpty()) {
            ClientInterceptor metadataInterceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
            builder.intercept(metadataInterceptor);
        }

        // set interceptors, gRPC invoke interceptors in reverse order
        beanFactory.getBeanProvider(ClientInterceptor.class).stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE.reversed())
                .forEach(builder::intercept);

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
            Optional.ofNullable(retry.getEnabled()).ifPresent((enabled -> {
                if (Boolean.TRUE.equals(enabled)) {
                    builder.enableRetry();
                } else {
                    builder.disableRetry();
                }
            }));
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
            Assert.hasText(
                    channelConfig.getInProcess().getName(),
                    "Not configure in-process name for stub: " + stubClass.getName());
            return InProcessChannelBuilder.forName(channelConfig.getInProcess().getName())
                    .directExecutor();
        }
        if (!StringUtils.hasText(channelConfig.getAuthority())) {
            throw new MissingChannelConfigurationException(stubClass);
        }

        // Priority: SSL Bundle > TLS > Plain text
        String sslBundleName = channelConfig.getSslBundle();
        GrpcClientProperties.Tls tls = channelConfig.getTls();

        if (StringUtils.hasText(sslBundleName)) {
            return createChannelWithSslBundle(channelConfig.getAuthority(), sslBundleName);
        } else if (tls != null) {
            logTlsDeprecationWarning();
            return createChannelWithTls(channelConfig.getAuthority(), tls);
        } else {
            return ManagedChannelBuilder.forTarget(channelConfig.getAuthority()).usePlaintext();
        }
    }

    private ManagedChannelBuilder<?> createChannelWithSslBundle(String authority, String sslBundleName) {
        SslBundles sslBundles = beanFactory.getBean(SslBundles.class);
        SslBundle sslBundle = sslBundles.getBundle(sslBundleName);

        TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder();

        // Set key managers if available
        if (sslBundle.getManagers().getKeyManagers() != null
                && sslBundle.getManagers().getKeyManagers().length > 0) {
            tlsBuilder.keyManager(sslBundle.getManagers().getKeyManagers());
        }

        // Set trust managers if available
        if (sslBundle.getManagers().getTrustManagers() != null
                && sslBundle.getManagers().getTrustManagers().length > 0) {
            tlsBuilder.trustManager(sslBundle.getManagers().getTrustManagers());
        }

        return Grpc.newChannelBuilder(authority, tlsBuilder.build());
    }

    @SneakyThrows
    private ManagedChannelBuilder<?> createChannelWithTls(String authority, GrpcClientProperties.Tls tls) {
        TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder();
        if (tls.getKeyManager() != null) {
            GrpcClientProperties.Tls.KeyManager keyManager = tls.getKeyManager();
            if (StringUtils.hasText(keyManager.getPrivateKeyPassword())) {
                tlsBuilder.keyManager(
                        keyManager.getCertChain().getInputStream(),
                        keyManager.getPrivateKey().getInputStream(),
                        keyManager.getPrivateKeyPassword());
            } else {
                tlsBuilder.keyManager(
                        keyManager.getCertChain().getInputStream(),
                        keyManager.getPrivateKey().getInputStream());
            }
        }
        if (tls.getTrustManager() != null) {
            tlsBuilder.trustManager(tls.getTrustManager().getRootCerts().getInputStream());
        }
        return Grpc.newChannelBuilder(authority, tlsBuilder.build());
    }

    private static void logTlsDeprecationWarning() {
        log.warn(
                """
                Using deprecated 'tls' configuration for gRPC client. \
                Please migrate to 'ssl-bundle' configuration. \
                The 'tls' configuration will be removed in a future version.""");
    }
}
