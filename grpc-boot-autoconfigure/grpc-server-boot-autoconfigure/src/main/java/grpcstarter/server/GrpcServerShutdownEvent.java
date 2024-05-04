package grpcstarter.server;

import io.grpc.Server;
import org.springframework.context.ApplicationEvent;

/**
 * Grpc server shutdown event, triggered when the gRPC server starts shutdown.
 *
 * @author Freeman
 */
public class GrpcServerShutdownEvent extends ApplicationEvent {

    public GrpcServerShutdownEvent(Server source) {
        super(source);
    }

    @Override
    public Server getSource() {
        return ((Server) super.getSource());
    }
}
