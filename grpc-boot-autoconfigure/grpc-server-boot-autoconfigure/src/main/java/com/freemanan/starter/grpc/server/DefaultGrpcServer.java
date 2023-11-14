package com.freemanan.starter.grpc.server;

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.TlsServerCredentials;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.GrpcUtil;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
            ObjectProvider<ServerBuilder<?>> serverBuilder,
            ObjectProvider<BindableService> serviceProvider,
            ObjectProvider<ServerInterceptor> interceptorProvider,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        this.properties = properties;
        this.server = buildGrpcServer(properties, serverBuilder, serviceProvider, interceptorProvider, customizers);
    }

    private static Server buildGrpcServer(
            GrpcServerProperties properties,
            ObjectProvider<ServerBuilder<?>> serverBuilder,
            ObjectProvider<BindableService> serviceProvider,
            ObjectProvider<ServerInterceptor> interceptorProvider,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        ServerBuilder<?> builder = serverBuilder.getIfUnique(() -> getDefaultServerBuilder(properties));

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

    @SneakyThrows
    private static ServerBuilder<? extends ServerBuilder<?>> getDefaultServerBuilder(GrpcServerProperties properties) {
        if (properties.getInProcess() != null) {
            Assert.hasText(properties.getInProcess().getName(), "In-process server name must not be empty");
            return InProcessServerBuilder.forName(properties.getInProcess().getName())
                    .directExecutor();
        }
        int port = Math.max(properties.getPort(), 0);
        GrpcServerProperties.Tls tls = properties.getTls();
        if (tls == null) {
            return ServerBuilder.forPort(port);
        }
        TlsServerCredentials.Builder tlsBuilder = TlsServerCredentials.newBuilder();
        GrpcServerProperties.Tls.KeyManager keyManager = tls.getKeyManager();
        if (keyManager != null) {
            if (StringUtils.hasText(keyManager.getPrivateKeyPassword())) {
                tlsBuilder.keyManager(
                        keyManager.getCertChain().getInputStream(),
                        keyManager.getPrivateKey().getInputStream(),
                        keyManager.getPrivateKeyPassword());
            } else {
                tlsBuilder.keyManager(
                        keyManager.getCertChain().getInputStream(),
                        keyManager.getPrivateKey().getInputStream());
            }
        }
        GrpcServerProperties.Tls.TrustManager trustManager = tls.getTrustManager();
        if (trustManager != null) {
            tlsBuilder.trustManager(trustManager.getRootCerts().getInputStream());
        }
        return Grpc.newServerBuilderForPort(port, tlsBuilder.build());
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
            gracefulShutdown();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Nullable
    @Override
    public Object getServer() {
        return server;
    }

    @Override
    public void stop() {
        if (isRunning.get()) {
            gracefulShutdown();
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
                                // wait here until terminating
                                latch.await();
                            } catch (InterruptedException e) {
                                log.warn("gRPC server await termination interrupted", e);
                                Thread.currentThread().interrupt();
                            }
                        },
                        "grpc-termination-awaiter")
                .start();
    }

    private void gracefulShutdown() {
        Duration timeout = Duration.ofMillis(properties.getShutdownTimeout());

        // stop accepting new calls
        server.shutdown();

        // publish shutdown event, user can listen to the event to complete the StreamObserver manually
        publisher.publishEvent(new GrpcServerShutdownEvent(server));

        long start = System.currentTimeMillis();
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

        publisher.publishEvent(new GrpcServerTerminatedEvent(server));

        if (log.isInfoEnabled()) {
            log.info("gRPC server graceful shutdown in {} ms", System.currentTimeMillis() - start);
        }
    }
}
