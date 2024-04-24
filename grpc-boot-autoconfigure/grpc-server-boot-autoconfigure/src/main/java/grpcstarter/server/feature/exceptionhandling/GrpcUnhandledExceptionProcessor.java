package grpcstarter.server.feature.exceptionhandling;

import io.grpc.Metadata;
import io.grpc.ServerCall;

/**
 * Process unhandled exception.
 *
 * <p> Generally used for exception reporting.
 *
 * @author Freeman
 */
public interface GrpcUnhandledExceptionProcessor {

    /**
     * Process unhandled exception.
     *
     * @param e       unhandled exception
     * @param call    server call
     * @param headers headers
     */
    void process(Throwable e, ServerCall<?, ?> call, Metadata headers);
}
