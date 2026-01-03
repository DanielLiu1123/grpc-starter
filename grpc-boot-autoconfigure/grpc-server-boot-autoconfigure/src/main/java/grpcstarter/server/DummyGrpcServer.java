package grpcstarter.server;

import org.jspecify.annotations.Nullable;

/**
 * Dummy gRPC server.
 *
 * @author Freeman
 */
public class DummyGrpcServer implements GrpcServer {
    public static final int DUMMY_PORT = -1;

    @Override
    public void start() {
        // nothing to do
    }

    @Override
    public void stop() {
        // nothing to do
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public int getPort() {
        return DUMMY_PORT;
    }

    @Override
    public @Nullable Object getServer() {
        return null;
    }
}
