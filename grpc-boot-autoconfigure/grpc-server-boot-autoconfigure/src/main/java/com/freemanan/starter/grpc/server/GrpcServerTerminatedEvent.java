package com.freemanan.starter.grpc.server;

import io.grpc.Server;
import org.springframework.context.ApplicationEvent;

/**
 * Grpc server terminated event, triggered when the gRPC server is terminated.
 *
 * @author Freeman
 */
public class GrpcServerTerminatedEvent extends ApplicationEvent {

    public GrpcServerTerminatedEvent(Server source) {
        super(source);
    }

    @Override
    public Server getSource() {
        return ((Server) super.getSource());
    }
}
