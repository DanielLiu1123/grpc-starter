package grpcstarter.client;

import grpcstarter.server.GrpcServerShutdownEvent;
import org.springframework.context.ApplicationListener;

/**
 * The operation of closing channels is executed when the gRPC server triggers the {@link GrpcServerShutdownEvent},
 * the server no longer accepts new requests at that time.
 *
 * @author Freeman
 */
public class ShutdownEventBasedChannelCloser implements ApplicationListener<GrpcServerShutdownEvent> {

    @Override
    public void onApplicationEvent(GrpcServerShutdownEvent event) {
        Cache.shutdownChannels();
    }
}
