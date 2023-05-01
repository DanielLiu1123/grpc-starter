package com.freemanan.starter.grpc.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.GrpcUtil;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * gRPC server.
 *
 * @author Freeman
 */
public class GrpcServer implements SmartLifecycle, ApplicationEventPublisherAware {

    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    private final Server server;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final CountDownLatch latch = new CountDownLatch(1);
    private final GrpcServerProperties properties;

    private ApplicationEventPublisher publisher;

    public GrpcServer(
            GrpcServerProperties properties,
            ObjectProvider<BindableService> serviceProvider,
            ObjectProvider<ServerInterceptor> interceptorProvider,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        this.properties = properties;
        this.server = buildGrpcServer(properties, serviceProvider, interceptorProvider, customizers);
    }

    private static Server buildGrpcServer(
            GrpcServerProperties properties,
            ObjectProvider<BindableService> serviceProvider,
            ObjectProvider<ServerInterceptor> interceptorProvider,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        ServerBuilder<?> builder;
        if (properties.getInProcess() == null) {
            builder = ServerBuilder.forPort(Math.max(properties.getPort(), 0));
        } else {
            Assert.hasText(properties.getInProcess().getName(), "In-process server name must not be empty");
            builder = InProcessServerBuilder.forName(properties.getInProcess().getName())
                    .directExecutor();
        }

        // add services
        serviceProvider.forEach(builder::addService);

        // add interceptors
        List<ServerInterceptor> interceptors =
                interceptorProvider.orderedStream().collect(Collectors.toList());
        // grpc invoke interceptor in reverse order
        Collections.reverse(interceptors);
        interceptors.forEach(builder::intercept);

        builder.maxInboundMessageSize((int) properties.getMaxMessageSize().toBytes());
        builder.maxInboundMetadataSize((int) properties.getMaxMetadataSize().toBytes());

        // apply customizers
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));

        return builder.build();
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        try {
            server.start();
            isRunning.set(true);
            log.info("gRPC server started on port: {} ({})", server.getPort(), GrpcUtil.getGrpcBuildVersion());

            publisher.publishEvent(new GrpcServerStartedEvent(server));
            waitUntilShutdown();
        } catch (IOException e) {
            gracefulShutdown(server, Duration.ofMillis(properties.getShutdownTimeout()));
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (isRunning.get()) {
            gracefulShutdown(server, Duration.ofMillis(properties.getShutdownTimeout()));
            isRunning.set(false);
            latch.countDown();
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    private void waitUntilShutdown() {
        new Thread(
                        () -> {
                            try {
                                // wait here until terminate
                                latch.await();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        "grpc-termination-awaiter")
                .start();
    }

    private static void gracefulShutdown(Server server, Duration timeout) {
        long start = System.currentTimeMillis();
        server.shutdown();
        try {
            long time = timeout.toMillis();
            if (time > 0L) {
                server.awaitTermination(time, TimeUnit.MILLISECONDS);
            } else {
                server.awaitTermination();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("gRPC server await termination interrupted", e);
        }
        if (!server.isTerminated()) {
            server.shutdownNow();
        }
        log.info("gRPC server graceful shutdown in {} ms", System.currentTimeMillis() - start);
    }
}
