package com.freemanan.starter.grpc.extensions.discovery;

import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Freeman
 */
public interface GrpcChannelTransformer {

    ManagedChannelBuilder<?> transformChannel(
            ManagedChannelBuilder<?> managedChannelBuilder, ServiceInstance serviceInstance);
}
