package com.freemanan.starter.grpc.client;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import java.lang.reflect.Method;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
class GrpcClientCreator {
    private static final String NEW_BLOCKING_STUB_METHOD = "newBlockingStub";
    private static final String NEW_FUTURE_STUB_METHOD = "newFutureStub";
    private static final String NEW_STUB_METHOD = "newStub";

    private final ConfigurableBeanFactory beanFactory;
    private final Class<?> stubClass;
    private final GrpcClientProperties.Channel channelConfig;

    GrpcClientCreator(ConfigurableBeanFactory beanFactory, GrpcClientProperties properties, Class<?> stubClass) {
        Assert.notNull(beanFactory, "beanFactory must not be null");
        Assert.notNull(properties, "properties must not be null");
        Assert.notNull(stubClass, "clientType must not be null");
        this.beanFactory = beanFactory;
        this.stubClass = stubClass;
        this.channelConfig = Util.findMatchedConfig(stubClass, properties).orElseGet(properties::defaultChannel);
    }

    /**
     * Create a gRPC stub instance.
     *
     * @param <T> stub type
     * @return gRPC stub instance
     */
    @SuppressWarnings("unchecked")
    public <T> T create() {
        Method stubMethod =
                ReflectionUtils.findMethod(stubClass.getEnclosingClass(), getStubMethodName(stubClass), Channel.class);
        Assert.notNull(stubMethod, "stubMethod must not be null");
        ManagedChannel chan = buildChannel();
        Cache.addChannel(new Chan(channelConfig, chan));
        T stub = (T) ReflectionUtils.invokeMethod(stubMethod, null, chan);
        Cache.addStubClass(stubClass);
        return stub;
    }

    private ManagedChannel buildChannel() {
        ManagedChannelBuilder<?> builder;
        if (channelConfig.getInProcess() == null) {
            // TODO(Freeman): use FailureAnalyzer instead of assert ?
            Assert.hasText(channelConfig.getAuthority(), "Not configure authority for stub: " + stubClass.getName());
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

    private static String getStubMethodName(Class<?> stubClass) {
        if (stubClass.getName().endsWith("BlockingStub")) {
            return NEW_BLOCKING_STUB_METHOD;
        } else if (stubClass.getName().endsWith("FutureStub")) {
            return NEW_FUTURE_STUB_METHOD;
        } else {
            return NEW_STUB_METHOD;
        }
    }
}
