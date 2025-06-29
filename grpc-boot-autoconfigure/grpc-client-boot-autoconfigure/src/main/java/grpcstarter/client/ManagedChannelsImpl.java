package grpcstarter.client;

import io.grpc.ManagedChannel;
import org.springframework.beans.factory.BeanFactory;

/**
 * Default implementation of {@link ManagedChannels}.
 *
 * @author Freeman
 * @since 3.5.3
 */
class ManagedChannelsImpl implements ManagedChannels {

    private final BeanFactory beanFactory;
    private final GrpcClientProperties properties;

    ManagedChannelsImpl(BeanFactory beanFactory, GrpcClientProperties properties) {
        this.beanFactory = beanFactory;
        this.properties = properties;
    }

    @Override
    public ManagedChannel getChannel(String name) {
        var channelConfig = Util.findChannelByName(name, properties);
        if (channelConfig == null) {
            throw new IllegalArgumentException("No channel found with name: " + name);
        }
        return GrpcClientUtil.createChannel(beanFactory, channelConfig);
    }
}
