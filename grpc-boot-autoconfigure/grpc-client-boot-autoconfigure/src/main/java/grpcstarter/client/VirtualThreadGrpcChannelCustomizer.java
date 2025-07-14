package grpcstarter.client;

import io.grpc.ManagedChannelBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * A {@link GrpcChannelCustomizer} that configures the gRPC channel to use virtual threads
 * when {@code spring.threads.virtual.enabled} is set to {@code true}.
 *
 * @author Freeman
 * @since 3.5.3.3
 */
public class VirtualThreadGrpcChannelCustomizer implements GrpcChannelCustomizer, Ordered {

    /**
     * This priority should be relatively high, allowing user to customize the executor.
     */
    public static final int ORDER = -1000;

    @Override
    public void customize(GrpcClientProperties.Channel channelConfig, ManagedChannelBuilder<?> channelBuilder) {
        channelBuilder.executor(new VirtualThreadTaskExecutor("grpc-channel-" + channelConfig.getName() + "-"));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
