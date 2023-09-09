package com.freemanan.starter.grpc.extensions.discovery;

import static com.freemanan.starter.grpc.client.GrpcChannelUtil.shutdownChannel;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import reactor.core.publisher.Mono;

/**
 * @author Freeman
 */
@RequiredArgsConstructor
public class LoadBalancerClientInterceptor implements ClientInterceptor, DisposableBean {

    private static final int CACHE_TIMEOUT = 120 * 1000;

    private final LoadBalancerClientFactory loadBalancerClientFactory;
    private final List<GrpcChannelTransformer> channelTransformers;
    private final LRUCache<String, ManagedChannel> authorityToChannelCache = new LRUCache<>(
            16, 30000, (authority, managedChannel) -> shutdownChannel(managedChannel, Duration.ofSeconds(5)));

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        String service = next.authority();
        ReactiveLoadBalancer<ServiceInstance> instances = loadBalancerClientFactory.getInstance(service);
        Response<ServiceInstance> response = Mono.from(instances.choose()).block();
        if (response == null || response.getServer() == null) {
            throw new IllegalStateException("No instances available for " + service);
        }
        ServiceInstance instance = response.getServer();

        String authority = instance.getHost() + ":" + instance.getPort();

        ManagedChannel newChannel = authorityToChannelCache.getOrSupply(
                authority,
                () -> {
                    ManagedChannelBuilder<?> builder =
                            ManagedChannelBuilder.forTarget(authority).usePlaintext();
                    for (GrpcChannelTransformer channelTransformer : channelTransformers) {
                        builder = channelTransformer.transformChannel(builder, instance);
                    }
                    return builder.build();
                },
                CACHE_TIMEOUT);
        return newChannel.newCall(method, callOptions);
    }

    @Override
    public void destroy() throws Exception {
        authorityToChannelCache.clear();
    }
}
