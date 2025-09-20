package grpcstarter.server;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.TlsServerCredentials;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.GrpcUtil;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * gRPC server.
 *
 * @author Freeman
 */
public class DefaultGrpcServer implements GrpcServer, ApplicationEventPublisherAware {

    private static final Logger log = LoggerFactory.getLogger(DefaultGrpcServer.class);

    private final Server server;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final GrpcServerProperties properties;

    @SuppressWarnings("NullAway")
    private ApplicationEventPublisher publisher;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public DefaultGrpcServer(
            GrpcServerProperties properties,
            SslBundles sslBundles,
            ObjectProvider<ServerBuilder<?>> serverBuilder,
            ObjectProvider<BindableService> serviceProvider,
            ObjectProvider<ServerInterceptor> interceptorProvider,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        this.properties = properties;
        this.server = buildGrpcServer(
                properties, sslBundles, serverBuilder, serviceProvider, interceptorProvider, customizers);
    }

    private static Server buildGrpcServer(
            GrpcServerProperties properties,
            SslBundles sslBundles,
            ObjectProvider<ServerBuilder<?>> serverBuilder,
            ObjectProvider<BindableService> serviceProvider,
            ObjectProvider<ServerInterceptor> interceptorProvider,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        ServerBuilder<?> builder = serverBuilder.getIfUnique(() -> getDefaultServerBuilder(properties, sslBundles));

        // add services
        serviceProvider.forEach(builder::addService);

        // add interceptors, gRPC applies interceptors in reversed order
        interceptorProvider.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE.reversed())
                .forEach(builder::intercept);

        Optional.ofNullable(properties.getMaxInboundMessageSize())
                .map(DataSize::toBytes)
                .map(Long::intValue)
                .ifPresent(builder::maxInboundMessageSize);
        Optional.ofNullable(properties.getMaxInboundMetadataSize())
                .map(DataSize::toBytes)
                .map(Long::intValue)
                .ifPresent(builder::maxInboundMetadataSize);

        // apply customizers
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));

        return builder.build();
    }

    @SneakyThrows
    private static ServerBuilder<? extends ServerBuilder<?>> getDefaultServerBuilder(
            GrpcServerProperties properties, SslBundles sslBundles) {
        if (properties.getInProcess() != null) {
            Assert.hasText(properties.getInProcess().name(), "In-process server name must not be empty");
            return InProcessServerBuilder.forName(properties.getInProcess().name())
                    .directExecutor();
        }
        int port = Math.max(properties.getPort(), 0);

        // Priority: SSL Bundle > Plain text
        String sslBundleName = properties.getSslBundle();

        if (StringUtils.hasText(sslBundleName)) {
            return createServerBuilderWithSslBundle(port, sslBundleName, sslBundles);
        } else {
            return Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create());
        }
    }

    private static ServerBuilder<? extends ServerBuilder<?>> createServerBuilderWithSslBundle(
            int port, String sslBundleName, SslBundles sslBundles) {
        SslBundle sslBundle = sslBundles.getBundle(sslBundleName);

        TlsServerCredentials.Builder tlsBuilder = TlsServerCredentials.newBuilder();

        // Set key managers if available
        var keyManagers = sslBundle.getManagers().getKeyManagers();
        if (keyManagers.length > 0) {
            tlsBuilder.keyManager(keyManagers);
        }

        // Set trust managers if available
        var trustManagers = sslBundle.getManagers().getTrustManagers();
        if (trustManagers.length > 0) {
            tlsBuilder.trustManager(trustManagers);
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
                if (properties.getInProcess() != null
                        && StringUtils.hasText(properties.getInProcess().name())) {
                    log.info(
                            "gRPC in-process server started: {}",
                            properties.getInProcess().name());
                } else {
                    log.info("gRPC server started on port: {} ({})", server.getPort(), GrpcUtil.getGrpcBuildVersion());
                }
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
        Thread t = new Thread(
                () -> {
                    try {
                        // wait here until terminating
                        server.awaitTermination();
                    } catch (InterruptedException e) {
                        log.warn("gRPC server await termination interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                },
                "grpc-termination-awaiter");
        t.setDaemon(false);
        t.start();
    }

    private void gracefulShutdown() {
        long start = System.currentTimeMillis();

        // stop accepting new calls
        server.shutdown();

        // publish shutdown event, user can listen to the event to complete the StreamObserver manually
        publisher.publishEvent(new GrpcServerShutdownEvent(server));

        try {
            long time = properties.getShutdownTimeout();
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
