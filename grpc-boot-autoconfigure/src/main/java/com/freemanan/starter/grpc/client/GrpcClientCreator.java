package com.freemanan.starter.grpc.client;

import com.freemanan.starter.grpc.GrpcProperties;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
    private final Class<?> clientType;
    private final GrpcProperties.Client.Stub config;

    GrpcClientCreator(ConfigurableBeanFactory beanFactory, GrpcProperties.Client properties, Class<?> clientType) {
        Assert.notNull(beanFactory, "beanFactory must not be null");
        Assert.notNull(properties, "properties must not be null");
        Assert.notNull(clientType, "clientType must not be null");
        this.beanFactory = beanFactory;
        this.clientType = clientType;
        this.config = Util.findMatchedConfig(clientType, properties).orElseGet(properties::defaultClient);
    }

    /**
     * Create a gRPC stub instance.
     *
     * @param <T> stub type
     * @return gRPC stub instance
     */
    @SuppressWarnings("unchecked")
    public <T> T create() {
        Method stubMethod = ReflectionUtils.findMethod(
                clientType.getEnclosingClass(), getStubMethodName(clientType), Channel.class);
        Assert.notNull(stubMethod, "stubMethod must not be null");
        return (T) ReflectionUtils.invokeMethod(stubMethod, null, getOrCreateChannel());
    }

    private Channel getOrCreateChannel() {
        ReusableChannel chan = new ReusableChannel(
                config.getAuthority(), config.getMaxMessageSize(), config.getMaxMetadataSize(), config.getMetadata());
        return Cache.getChannel(chan, this::buildChannel);
    }

    private Channel buildChannel() {
        ManagedChannelBuilder<?> builder;
        if (config.getInProcess() == null) {
            Assert.hasText(config.getAuthority(), "Not configure authority for stub: " + clientType.getName());
            builder = ManagedChannelBuilder.forTarget(config.getAuthority());
        } else {
            Assert.hasText(
                    config.getInProcess().getName(), "Not configure in-process name for stub: " + clientType.getName());
            builder = InProcessChannelBuilder.forName(config.getInProcess().getName())
                    .directExecutor();
        }

        // set max message size and max metadata size
        builder.maxInboundMessageSize((int) config.getMaxMessageSize().toBytes());
        builder.maxInboundMetadataSize((int) config.getMaxMetadataSize().toBytes());

        // add default metadata
        Metadata metadata = new Metadata();
        config.getMetadata().forEach(m -> {
            Metadata.Key<String> key = Metadata.Key.of(m.getKey(), Metadata.ASCII_STRING_MARSHALLER);
            m.getValues().forEach(v -> metadata.put(key, v));
        });
        ClientInterceptor metadataInterceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
        builder.intercept(metadataInterceptor);

        // set interceptors, gRPC invoke interceptors in reverse order
        List<ClientInterceptor> interceptors = beanFactory
                .getBeanProvider(ClientInterceptor.class)
                .orderedStream()
                .collect(Collectors.toList());
        Collections.reverse(interceptors);
        builder.intercept(interceptors);

        // use plaintext
        builder.usePlaintext();

        // apply customizers
        beanFactory
                .getBeanProvider(GrpcChannelCustomizer.class)
                .orderedStream()
                .forEach(customizer -> customizer.customize(builder));

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
