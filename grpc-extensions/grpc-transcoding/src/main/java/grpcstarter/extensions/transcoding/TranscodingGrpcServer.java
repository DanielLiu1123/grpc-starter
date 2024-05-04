package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.Util.TRANSCODING_SERVER_IN_PROCESS_NAME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import grpcstarter.server.GrpcServerCustomizer;
import grpcstarter.server.GrpcServerProperties;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.unit.DataSize;

/**
 * @author Freeman
 * @since 3.3.0
 */
public class TranscodingGrpcServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(TranscodingGrpcServer.class);

    private final Server server;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final CountDownLatch latch = new CountDownLatch(1);
    private final GrpcServerProperties properties;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public TranscodingGrpcServer(
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
        ServerBuilder<?> builder = InProcessServerBuilder.forName(TRANSCODING_SERVER_IN_PROCESS_NAME);

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

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        try {
            server.start();
            isRunning.set(true);
            if (log.isInfoEnabled()) {
                log.info("gRPC transcoding in-process server started: {}", TRANSCODING_SERVER_IN_PROCESS_NAME);
            }

            waitUntilShutdown();
        } catch (IOException e) {
            gracefulShutdown();
            throw new IllegalStateException(e);
        }
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

    private void waitUntilShutdown() {
        new Thread(
                        () -> {
                            try {
                                // wait here until terminating
                                latch.await();
                            } catch (InterruptedException e) {
                                log.warn("gRPC transcoding server await termination interrupted", e);
                                Thread.currentThread().interrupt();
                            }
                        },
                        "grpc-transcoding-termination-awaiter")
                .start();
    }

    private void gracefulShutdown() {
        long start = System.currentTimeMillis();

        // stop accepting new calls
        server.shutdown();

        try {
            long time = properties.getShutdownTimeout();
            if (time > 0L) {
                server.awaitTermination(time, TimeUnit.MILLISECONDS);
            } else {
                server.awaitTermination();
            }
        } catch (InterruptedException e) {
            log.warn("gRPC transcoding server graceful shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }
        if (!server.isTerminated()) {
            server.shutdownNow();
        }

        if (log.isInfoEnabled()) {
            log.info("gRPC transcoding server graceful shutdown in {} ms", System.currentTimeMillis() - start);
        }
    }
}
