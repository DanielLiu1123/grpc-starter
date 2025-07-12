package grpcstarter.server;

import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * A {@link GrpcServerCustomizer} that configures the gRPC server to use virtual threads
 * when {@code spring.threads.virtual.enabled} is set to {@code true}.
 *
 * <p>Note: This customizer will not affect in-process servers, which continue to use
 * the direct executor for testing scenarios.
 *
 * @author Freeman
 * @since 3.5.3.2
 */
final class VirtualThreadGrpcServerCustomizer implements GrpcServerCustomizer {
    private static final Logger log = LoggerFactory.getLogger(VirtualThreadGrpcServerCustomizer.class);

    @Override
    public void customize(ServerBuilder<?> serverBuilder) {

        serverBuilder.executor(new VirtualThreadTaskExecutor("grpc-"));

        if (log.isDebugEnabled()) {
            log.debug("Configured gRPC server to use virtual threads");
        }
    }
}
