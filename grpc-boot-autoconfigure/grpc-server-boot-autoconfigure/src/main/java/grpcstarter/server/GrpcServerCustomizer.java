package grpcstarter.server;

import io.grpc.ServerBuilder;

/**
 * Grpc server customizer.
 *
 * @author Freeman
 */
public interface GrpcServerCustomizer {

    /**
     * Customize the server builder.
     *
     * @param serverBuilder the server builder to customize
     */
    void customize(ServerBuilder<?> serverBuilder);
}
