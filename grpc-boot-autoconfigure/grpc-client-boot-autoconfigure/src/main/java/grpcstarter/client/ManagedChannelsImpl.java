package grpcstarter.client;

import io.grpc.ManagedChannel;

/**
 * Default implementation of {@link ManagedChannels}.
 *
 * @author Freeman
 * @since 3.5.3
 */
class ManagedChannelsImpl implements ManagedChannels {

    @Override
    public ManagedChannel getChannel(String name) {
        ManagedChannel channel = Cache.getChannelByName(name);
        if (channel == null) {
            throw new IllegalArgumentException("No channel found with name: " + name);
        }
        return channel;
    }
}
