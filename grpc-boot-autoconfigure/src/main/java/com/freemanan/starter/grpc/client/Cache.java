package com.freemanan.starter.grpc.client;

import io.grpc.Channel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
class Cache {

    private static final ConcurrentMap<ReusableChannel, Channel> channels = new ConcurrentHashMap<>();
    private static final Set<Class<?>> clientClasses = ConcurrentHashMap.newKeySet();

    public static Channel getChannel(ReusableChannel chan, Supplier<Channel> supplier) {
        return channels.computeIfAbsent(chan, it -> supplier.get());
    }

    public static void addClientClass(Class<?> type) {
        clientClasses.add(type);
    }

    public static Set<Class<?>> getClientClasses() {
        return Set.copyOf(clientClasses);
    }

    public static void clear() {
        channels.clear();
        clientClasses.clear();
    }
}
