package com.freemanan.starter.grpc.server;

import org.springframework.context.SmartLifecycle;

/**
 * @author Freeman
 */
public interface GrpcServer extends SmartLifecycle {

    /**
     * Get the port the server is listening on.
     *
     * @return port number
     */
    int getPort();
}
