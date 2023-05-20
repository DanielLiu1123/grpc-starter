package com.freemanan.starter.grpc.extensions.xds;

import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.service.cluster.v3.ClusterDiscoveryServiceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
public class GrpcXdsAutoConfiguration {

    public static void main(String[] args) {
        V3DiscoveryServer ds =
                new V3DiscoveryServer(new SimpleCache<>(node1 -> String.valueOf(ObjectUtils.nullSafeHashCode(node1))));
        ClusterDiscoveryServiceGrpc.ClusterDiscoveryServiceImplBase cds = ds.getClusterDiscoveryServiceImpl();
        System.out.println(cds);
    }
}
