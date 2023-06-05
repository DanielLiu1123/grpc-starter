package com.freemanan.starter.grpc.client;

import io.grpc.ManagedChannel;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Messy utils for gRPC client module.
 *
 * @author Freeman
 */
@UtilityClass
class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    /**
     * Service name field name for gRPC service.
     */
    public static final String SERVICE_NAME = "SERVICE_NAME";

    public static Optional<GrpcClientProperties.Channel> findMatchedConfig(
            Class<?> clz, GrpcClientProperties properties) {
        // find from stub class first
        Optional<GrpcClientProperties.Channel> found = properties.getChannels().stream()
                .filter(chan -> chan.getStubs().stream().anyMatch(ch -> ch == clz))
                .findFirst();
        if (found.isPresent()) {
            return found;
        }
        // not class match, try to find from service name
        return properties.getChannels().stream().filter(it -> match(clz, it)).findFirst();
    }

    static boolean match(Class<?> stubClass, GrpcClientProperties.Channel channelConfig) {
        if (channelConfig.getStubs().stream().anyMatch(ch -> ch == stubClass)) {
            return true;
        }
        String service = serviceName(stubClass);
        return channelConfig.getServices().stream().anyMatch(svc -> Objects.equals(svc, service));
    }

    public static String serviceName(Class<?> stubClass) {
        Field serviceNameField = ReflectionUtils.findField(stubClass.getEnclosingClass(), SERVICE_NAME);
        Assert.notNull(serviceNameField, SERVICE_NAME + " field not found");
        return (String) ReflectionUtils.getField(serviceNameField, null);
    }

    public static GrpcClientProperties getProperties(Environment environment) {
        GrpcClientProperties properties = Binder.get(environment)
                .bind(GrpcClientProperties.PREFIX, GrpcClientProperties.class)
                .orElseGet(GrpcClientProperties::new);
        properties.merge();
        return properties;
    }

    public static void shutdownChannel(ManagedChannel channel, Duration timeout) {
        if (channel == null || channel.isTerminated()) {
            return;
        }

        long ms = timeout.toMillis();
        // Close the gRPC managed-channel if not shut down already.
        try {
            channel.shutdown();
            if (!channel.awaitTermination(ms, TimeUnit.MILLISECONDS)) {
                log.warn("Graceful shutdown timed out: {}ms, channel: {}", ms, channel);
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted gracefully shutting down channel: {}", channel);
            Thread.currentThread().interrupt();
        }

        // Forceful shut down if still not terminated.
        if (!channel.isTerminated()) {
            try {
                channel.shutdownNow();
                if (!channel.awaitTermination(15, TimeUnit.SECONDS)) {
                    log.warn("Forceful shutdown timed out: 15s, channel: {}. ", channel);
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted forcefully shutting down channel: {}. ", channel);
                Thread.currentThread().interrupt();
            }
        }
    }
}
