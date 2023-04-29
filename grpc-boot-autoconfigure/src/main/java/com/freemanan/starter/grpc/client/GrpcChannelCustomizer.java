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
     * @param channelBuilder the channel to customize
     */
    void customize(ManagedChannelBuilder<?> channelBuilder);
}
