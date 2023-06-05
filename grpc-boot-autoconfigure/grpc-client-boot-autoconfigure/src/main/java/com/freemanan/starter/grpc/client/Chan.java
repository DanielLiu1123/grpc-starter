package com.freemanan.starter.grpc.client;

import io.grpc.ManagedChannel;
import lombok.Data;

/**
 * @author Freeman
 */
@Data
class Chan {
    private final GrpcClientProperties.Channel channelConfig;
    private final ManagedChannel channel;
}
