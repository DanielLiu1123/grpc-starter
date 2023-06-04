package com.freemanan.starter.grpc.client;

import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
class Cache {

    /**
     * Cache service name to stub classes mapping of gRPC stubs that already created bean.
     */
    private static final ConcurrentMap<String, List<Class<?>>> serviceToStubClasses = new ConcurrentHashMap<>();

    private static final Set<ManagedChannel> channels = ConcurrentHashMap.newKeySet();

    public static Set<Class<?>> getStubClasses() {
        return serviceToStubClasses.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    public static Set<String> getServices() {
        return Collections.unmodifiableSet(serviceToStubClasses.keySet());
    }

    public static void addStubClass(Class<?> stubClass) {
        String service = Util.serviceName(stubClass);
        serviceToStubClasses.computeIfAbsent(service, k -> new ArrayList<>()).add(stubClass);
    }

    public static void addChannel(ManagedChannel channel) {
        channels.add(channel);
    }

    public static void shutdownChannels() {
        channels.forEach(c -> Util.shutdownChannel(c, Duration.ofSeconds(5)));
    }
}
