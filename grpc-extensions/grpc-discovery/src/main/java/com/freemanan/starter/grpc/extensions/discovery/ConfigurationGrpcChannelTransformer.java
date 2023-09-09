package com.freemanan.starter.grpc.extensions.discovery;

import com.freemanan.starter.grpc.client.GrpcClientProperties;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Freeman
 */
public class ConfigurationGrpcChannelTransformer implements GrpcChannelTransformer {

    private final Map<String, GrpcClientProperties.Channel> serviceToChannelConfig = new HashMap<>();

    public ConfigurationGrpcChannelTransformer(GrpcClientProperties grpcClientProperties) {
        this.serviceToChannelConfig.putAll(grpcClientProperties.getChannels().stream()
                .collect(Collectors.toMap(GrpcClientProperties.Channel::getAuthority, Function.identity())));
    }

    @Override
    public ManagedChannelBuilder<?> transformChannel(
            ManagedChannelBuilder<?> managedChannelBuilder, ServiceInstance serviceInstance) {
        String service = serviceInstance.getServiceId();
        GrpcClientProperties.Channel channelConfig = serviceToChannelConfig.get(service);
        if (channelConfig == null) {
            return managedChannelBuilder;
        }
        managedChannelBuilder
                .maxInboundMessageSize((int) channelConfig.getMaxMessageSize().toBytes())
                .maxInboundMetadataSize((int) channelConfig.getMaxMetadataSize().toBytes());
        return managedChannelBuilder;
    }
}
