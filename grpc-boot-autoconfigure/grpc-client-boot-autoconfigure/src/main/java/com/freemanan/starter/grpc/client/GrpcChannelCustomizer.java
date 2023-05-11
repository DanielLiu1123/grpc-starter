package com.freemanan.starter.grpc.client;

import io.grpc.ManagedChannelBuilder;

/**
 * gRPC {@link ManagedChannelBuilder} customizer.
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
