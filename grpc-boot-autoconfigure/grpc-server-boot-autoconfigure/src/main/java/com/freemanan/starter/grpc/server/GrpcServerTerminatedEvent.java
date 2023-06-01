package com.freemanan.starter.grpc.server;

import io.grpc.Server;
import org.springframework.context.ApplicationEvent;

/**
 * Grpc server started event.
 *
 * <p> Can use to get the random server port.
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
