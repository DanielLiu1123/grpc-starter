package com.freemanan.starter.grpc.server;

/**
 * Dummy gRPC server.
 *
 * @author Freeman
 */
public class DummyGrpcServer implements GrpcServer {

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
}
