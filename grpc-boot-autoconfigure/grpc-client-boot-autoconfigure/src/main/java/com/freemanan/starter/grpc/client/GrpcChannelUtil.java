package com.freemanan.starter.grpc.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Messy utils for gRPC client module.
 *
 * @author Freeman
 */
@UtilityClass
public class GrpcChannelUtil {
    private static final Logger log = LoggerFactory.getLogger(GrpcChannelUtil.class);

    /**
     * Shutdown the gRPC managed-channel gracefully.
     *
     * @param channel         gRPC managed-channel
     * @param shutdownTimeout timeout for graceful shutdown
     */
    public static void shutdownChannel(Channel channel, Duration shutdownTimeout) {
        if (!(channel instanceof ManagedChannel)) {
            return;
        }
        ManagedChannel ch = (ManagedChannel) channel;
        if (ch.isTerminated()) {
            return;
        }

        long ms = shutdownTimeout.toMillis();
        // Close the gRPC managed-channel if not shut down already.
        try {
            ch.shutdown();
            if (!ch.awaitTermination(ms, TimeUnit.MILLISECONDS)) {
                log.warn("Graceful shutdown timed out: {}ms, channel: {}", ms, ch);
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted gracefully shutting down channel: {}", ch);
            Thread.currentThread().interrupt();
        }

        // Forcefully shut down if still not terminated.
        if (!ch.isTerminated()) {
            try {
                ch.shutdownNow();
                if (!ch.awaitTermination(15, TimeUnit.SECONDS)) {
                    log.warn("Forcefully shutdown timed out: 15s, channel: {}. ", ch);
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted forcefully shutting down channel: {}. ", ch);
                Thread.currentThread().interrupt();
            }
        }
    }
}
