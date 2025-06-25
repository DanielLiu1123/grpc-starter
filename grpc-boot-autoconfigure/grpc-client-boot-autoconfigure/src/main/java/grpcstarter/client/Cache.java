package grpcstarter.client;

import static grpcstarter.client.Util.serviceName;
import static grpcstarter.client.Util.shutdownChannel;

import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
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

        // Also cache by name if the channel has a name
        String channelName = channelConfig.getName();
        if (channelName != null && !channelName.isEmpty() && !channelName.equals("__default__")) {
            nameToChannel.put(channelName, channel);
        }

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
    }

    /**
     * Get a ManagedChannel by name.
     *
     * @param name the channel name
     * @return the managed channel, or null if not found
     */
    public static ManagedChannel getChannelByName(String name) {
        return nameToChannel.get(name);
    }

    /**
     * Get all available channel names.
     *
     * @return a set of channel names
     */
    public static Set<String> getChannelNames() {
        return Collections.unmodifiableSet(nameToChannel.keySet());
    }

    /**
     * Check if a channel with the given name exists.
     *
     * @param name the channel name
     * @return true if the channel exists, false otherwise
     */
    public static boolean hasChannel(String name) {
        return nameToChannel.containsKey(name);
    }

    public static void clear() {
        serviceToStubClasses.clear();
        nameToChannel.clear();
        shutdownChannels();
    }
}
