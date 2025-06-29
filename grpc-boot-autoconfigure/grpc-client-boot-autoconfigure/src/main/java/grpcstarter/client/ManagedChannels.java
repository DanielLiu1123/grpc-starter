package grpcstarter.client;

import io.grpc.ManagedChannel;

/**
 * A collection of {@link ManagedChannel} instances that created by framework.
 *
 * <p>This interface provides a centralized way to access configured gRPC channels,
 * similar to how Spring's {@code SslBundles} works.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Bean
 * MyServiceBlockingStub stub1(ManagedChannels channels) {
 *     ManagedChannel channel = channels.getChannel("channel-1");
 *     return MyServiceGrpc.newBlockingStub(channel);
 * }
 * }</pre>
 *
 * @author Freeman
 * @since 3.5.3
 */
public interface ManagedChannels {

    /**
     * Get a {@link ManagedChannel} by name.
     *
     * @param name the channel name
     * @return the managed channel
     * @throws IllegalArgumentException if no channel with the given name exists
     */
    ManagedChannel getChannel(String name);
}
