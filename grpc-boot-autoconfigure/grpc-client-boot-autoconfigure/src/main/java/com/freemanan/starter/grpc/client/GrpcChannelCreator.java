package com.freemanan.starter.grpc.client;

import com.freemanan.starter.grpc.client.exception.MissingChannelConfigurationException;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
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
    private final GrpcClientProperties properties;

    GrpcChannelCreator(BeanFactory beanFactory, Class<?> stubClass, GrpcClientProperties properties) {
        this.beanFactory = beanFactory;
        this.stubClass = stubClass;
        this.properties = properties;
    }

    public ManagedChannel create() {
        GrpcClientProperties grpcClientProperties = beanFactory
                .getBeanProvider(GrpcClientProperties.class) // get from beanFactory first because it can be refreshed
                .getIfUnique(() -> properties);

        GrpcClientProperties.Channel channelConfig =
                Util.findMatchedConfig(stubClass, grpcClientProperties).orElseGet(grpcClientProperties::defaultChannel);

        ManagedChannel channel = buildChannel(channelConfig);

        Cache.addChannel(channelConfig, channel);

        return channel;
    }

    private ManagedChannel buildChannel(GrpcClientProperties.Channel channelConfig) {
        ManagedChannelBuilder<?> builder;
        if (channelConfig.getInProcess() == null) {
            if (!StringUtils.hasText(channelConfig.getAuthority())) {
                throw new MissingChannelConfigurationException(stubClass);
            }
            builder = ManagedChannelBuilder.forTarget(channelConfig.getAuthority());
        } else {
            Assert.hasText(
                    channelConfig.getInProcess().getName(),
                    "Not configure in-process name for stub: " + stubClass.getName());
            builder = InProcessChannelBuilder.forName(
                            channelConfig.getInProcess().getName())
                    .directExecutor();
        }

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

        // use plaintext
        builder.usePlaintext();

        // apply customizers
        beanFactory
                .getBeanProvider(GrpcChannelCustomizer.class)
                .orderedStream()
                .forEach(cc -> cc.customize(channelConfig, builder));

        return builder.build();
    }
}
