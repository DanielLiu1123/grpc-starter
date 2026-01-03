package grpcstarter.server;

import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;

/**
 * gRPC server.
 *
 * @author Freeman
 */
public interface GrpcServer extends SmartLifecycle {

    /**
     * Get the port the server is listening on.
     *
     * @return port number
     */
    int getPort();

    /**
     * Get the server object.
     */
    @Nullable Object getServer();
}
