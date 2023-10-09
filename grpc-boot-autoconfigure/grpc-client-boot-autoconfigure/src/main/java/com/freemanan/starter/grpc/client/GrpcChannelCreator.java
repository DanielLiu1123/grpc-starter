package com.freemanan.starter.grpc.client;

import com.freemanan.starter.grpc.client.exception.MissingChannelConfigurationException;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.TlsChannelCredentials;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
class GrpcChannelCreator {

    private final BeanFactory beanFactory;
    private final Class<?> stubClass;

    GrpcChannelCreator(BeanFactory beanFactory, Class<?> stubClass) {
        this.beanFactory = beanFactory;
        this.stubClass = stubClass;
    }

    public ManagedChannel create() {
        GrpcClientProperties grpcClientProperties = beanFactory.getBean(
                GrpcClientProperties.class); // get from beanFactory first because it can be refreshed

        GrpcClientProperties.Channel channelConfig =
                Util.findMatchedConfig(stubClass, grpcClientProperties).orElseGet(grpcClientProperties::defaultChannel);

        // One channel configuration results in the creation of one gRPC channel.
        // See https://github.com/DanielLiu1123/grpc-starter/issues/23
        return Cache.getOrSupplyChannel(channelConfig, () -> buildChannel(channelConfig));
    }

    private ManagedChannel buildChannel(GrpcClientProperties.Channel channelConfig) {
        ManagedChannelBuilder<?> builder = getManagedChannelBuilder(channelConfig);

        // set max message size and max metadata size
        builder.maxInboundMessageSize((int) channelConfig.getMaxMessageSize().toBytes());
        builder.maxInboundMetadataSize((int) channelConfig.getMaxMetadataSize().toBytes());

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
        GrpcClientProperties.Tls tls = channelConfig.getTls();
        if (tls == null) {
            return ManagedChannelBuilder.forTarget(channelConfig.getAuthority()).usePlaintext();
        }
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
        return Grpc.newChannelBuilder(channelConfig.getAuthority(), tlsBuilder.build());
    }
}
