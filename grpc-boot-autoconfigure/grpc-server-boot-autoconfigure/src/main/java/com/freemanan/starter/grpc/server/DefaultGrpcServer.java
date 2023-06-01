package com.freemanan.starter.grpc.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.GrpcUtil;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * gRPC server.
 *
 * @author Freeman
 */
public class DefaultGrpcServer implements GrpcServer, ApplicationEventPublisherAware {

    private static final Logger log = LoggerFactory.getLogger(DefaultGrpcServer.class);

    private final Server server;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final CountDownLatch latch = new CountDownLatch(1);
    private final GrpcServerProperties properties;

    private ApplicationEventPublisher publisher;

    public DefaultGrpcServer(
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

        // add interceptors, gRPC applies interceptors in reversed order
        interceptorProvider.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE.reversed())
                .forEach(builder::intercept);

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
            if (log.isInfoEnabled()) {
                log.info("gRPC server started on port: {} ({})", server.getPort(), GrpcUtil.getGrpcBuildVersion());
            }

            publisher.publishEvent(new GrpcServerStartedEvent(server));
            waitUntilShutdown();
        } catch (IOException e) {
            gracefulShutdown(server, Duration.ofMillis(properties.getShutdownTimeout()));
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        if (isRunning.get()) {
            gracefulShutdown(server, Duration.ofMillis(properties.getShutdownTimeout()));
            isRunning.set(false);
            latch.countDown();

            publisher.publishEvent(new GrpcServerTerminatedEvent(server));
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
                                log.warn("gRPC server await termination interrupted", e);
                                Thread.currentThread().interrupt();
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
            log.warn("gRPC server graceful shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }
        if (!server.isTerminated()) {
            server.shutdownNow();
        }
        if (log.isInfoEnabled()) {
            log.info("gRPC server graceful shutdown in {} ms", System.currentTimeMillis() - start);
        }
    }
}
