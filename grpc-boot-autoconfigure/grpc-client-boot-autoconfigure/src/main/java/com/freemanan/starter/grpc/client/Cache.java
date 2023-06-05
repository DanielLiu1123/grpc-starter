package com.freemanan.starter.grpc.client;

import static com.freemanan.starter.grpc.client.Util.serviceName;
import static com.freemanan.starter.grpc.client.Util.shutdownChannel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Freeman
 */
@UtilityClass
class Cache {
    private static final Logger log = LoggerFactory.getLogger(Cache.class);

    /**
     * Cache service name to stub classes mapping of gRPC stubs that already created bean.
     */
    private static final ConcurrentMap<String, List<Class<?>>> serviceToStubClasses = new ConcurrentHashMap<>();

    private static final Set<Chan> channels = Collections.synchronizedSet(new LinkedHashSet<>());

    public static Set<Class<?>> getStubClasses() {
        return serviceToStubClasses.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    public static Set<String> getServices() {
        return Collections.unmodifiableSet(serviceToStubClasses.keySet());
    }

    public static void addStubClass(Class<?> stubClass) {
        String service = serviceName(stubClass);
        serviceToStubClasses.computeIfAbsent(service, k -> new ArrayList<>()).add(stubClass);
    }

    public static void addChannel(Chan channel) {
        channels.add(channel);
    }

    /**
     * Shutdown all channels, then clear the cache.
     */
    public static void shutdownChannels() {
        if (channels.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        channels.forEach(ch -> shutdownChannel(
                ch.getChannel(), Duration.ofMillis(ch.getChannelConfig().getShutdownTimeout())));
        if (log.isInfoEnabled()) {
            log.info("{} channels gracefully shutdown in {} ms", channels.size(), System.currentTimeMillis() - start);
        }
        channels.clear();
    }
}
