package grpcstarter.client;

import static grpcstarter.client.Util.serviceName;
import static grpcstarter.client.Util.shutdownChannel;

import io.grpc.ManagedChannel;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
final class Cache {

    private Cache() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    private static final Logger log = LoggerFactory.getLogger(Cache.class);

    private static final AtomicInteger unnamedChannelCounter = new AtomicInteger(0);

    /**
     * Cache service name to stub classes mapping of gRPC stubs that already created bean.
     */
    private static final ConcurrentMap<String, List<Class<?>>> serviceToStubClasses = new ConcurrentHashMap<>();

    private static final Map<GrpcClientProperties.Channel, ManagedChannel> cfgToChannel =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Cache channel name to ManagedChannel mapping.
     */
    private static final ConcurrentMap<String, ManagedChannel> nameToChannel = new ConcurrentHashMap<>();

    public static Set<Class<?>> getStubClasses() {
        return serviceToStubClasses.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    /**
     * @return all service names that already created stub beans.
     */
    public static Set<String> getServices() {
        return Collections.unmodifiableSet(serviceToStubClasses.keySet());
    }

    public static void addStubClass(Class<?> stubClass) {
        String service = serviceName(stubClass);
        serviceToStubClasses.computeIfAbsent(service, k -> new ArrayList<>()).add(stubClass);
    }

    public static ManagedChannel getOrSupplyChannel(
            GrpcClientProperties.Channel channelConfig, Supplier<ManagedChannel> channelSupplier) {
        // Do not close the channel if it already exists, it may be still in use
        ManagedChannel channel = cfgToChannel.computeIfAbsent(channelConfig, k -> channelSupplier.get());

        // Also cache by name
        String channelName = channelConfig.getName();
        if (!StringUtils.hasText(channelName)) {
            throw new IllegalArgumentException(
                    "Channel name must not be null or empty, authority: " + channelConfig.getAuthority());
        }

        nameToChannel.put(channelName, channel);

        return channel;
    }

    /**
     * Shutdown all channels, then clear the cache.
     */
    public static void shutdownChannels() {
        if (cfgToChannel.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        cfgToChannel.forEach((cfg, channel) -> {
            Long timeout = cfg.getShutdownTimeout();
            Duration duration = timeout != null ? Duration.ofMillis(timeout) : Duration.ofMillis(5000L);
            shutdownChannel(channel, duration);
        });
        if (log.isInfoEnabled()) {
            log.info(
                    "{} channels gracefully shutdown in {} ms",
                    cfgToChannel.size(),
                    System.currentTimeMillis() - start);
        }
        cfgToChannel.clear();
        nameToChannel.clear();
    }

    /**
     * Get a ManagedChannel by name.
     *
     * @param name the channel name
     * @return the managed channel, or null if not found
     */
    @Nullable
    public static ManagedChannel getChannelByName(String name) {
        return nameToChannel.get(name);
    }

    public static void clear() {
        serviceToStubClasses.clear();
        shutdownChannels();
        unnamedChannelCounter.set(0);
    }
}
