package grpcstarter.client;

import io.grpc.ManagedChannel;
import java.util.Set;

/**
 * Default implementation of {@link ManagedChannels}.
 *
 * @author Freeman
 * @since 3.5.3
 */
class ManagedChannelsImpl implements ManagedChannels {

    @Override
    public ManagedChannel getChannel(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Channel name must not be null or empty");
        }

        ManagedChannel channel = Cache.getChannelByName(name);
        if (channel == null) {
            throw new IllegalArgumentException("No channel found with name: " + name);
        }

        return channel;
    }

    @Override
    public Set<String> getChannelNames() {
        return Cache.getChannelNames();
    }

    @Override
    public boolean hasChannel(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return Cache.hasChannel(name);
    }
}
