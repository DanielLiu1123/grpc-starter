package grpcstarter.client;

import io.grpc.ManagedChannelBuilder;

/**
 * The interface {@link GrpcChannelCustomizer} represents a mechanism for customizing a gRPC channel.
 * Implementations of this interface can customize the channel by modifying the channel configuration,
 * such as setting timeouts or adding interceptors.
 *
 * @author Freeman
 */
public interface GrpcChannelCustomizer {

    /**
     * Customize the given {@link ManagedChannelBuilder}.
     *
     * @param channelConfig  current channel configuration
     * @param channelBuilder the channel to customize
     */
    void customize(GrpcClientProperties.Channel channelConfig, ManagedChannelBuilder<?> channelBuilder);
}
