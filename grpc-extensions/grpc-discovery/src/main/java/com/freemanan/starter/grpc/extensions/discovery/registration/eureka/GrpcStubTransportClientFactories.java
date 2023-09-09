package com.freemanan.starter.grpc.extensions.discovery.registration.eureka;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import java.util.Collection;
import java.util.Optional;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * @author Freeman
 */
public class GrpcStubTransportClientFactories implements TransportClientFactories<Void> {
    @Override
    public TransportClientFactory newTransportClientFactory(
            EurekaClientConfig clientConfig, Collection<Void> additionalFilters, InstanceInfo myInstanceInfo) {
        return null;
    }

    @Override
    public TransportClientFactory newTransportClientFactory(
            EurekaClientConfig clientConfig,
            Collection<Void> additionalFilters,
            InstanceInfo myInstanceInfo,
            Optional<SSLContext> sslContext,
            Optional<HostnameVerifier> hostnameVerifier) {
        return null;
    }
}
