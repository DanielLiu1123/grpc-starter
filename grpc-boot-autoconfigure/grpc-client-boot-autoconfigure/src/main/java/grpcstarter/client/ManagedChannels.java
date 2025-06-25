package grpcstarter.client;

import io.grpc.ManagedChannel;
import java.util.Set;

/**
 * A collection of {@link ManagedChannel} instances that can be queried by name.
 *
 * <p>This interface provides a centralized way to access configured gRPC channels,
 * similar to how Spring's {@code SslBundles} works.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Bean("realm-1")
 * MyServiceBlockingStub realm1Bean(ManagedChannels channels) {
 *     ManagedChannel channel = channels.getChannel("realm-1");
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

    /**
     * Get all available channel names.
     *
     * @return a set of channel names
     */
    Set<String> getChannelNames();

    /**
     * Check if a channel with the given name exists.
     *
     * @param name the channel name
     * @return true if the channel exists, false otherwise
     */
    boolean hasChannel(String name);
}
