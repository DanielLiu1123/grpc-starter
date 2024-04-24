package grpcstarter.server.feature.exceptionhandling.annotation;

import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Handle exceptions recognized by gRPC.
 *
 * <ul>
 *     <li>{@link StatusRuntimeException}</li>
 *     <li>{@link StatusException}</li>
 * </ul>
 *
 * @author Freeman
 * @since 3.2.3
 */
@GrpcAdvice
public class DefaultGrpcExceptionAdvice {

    @GrpcExceptionHandler
    public StatusRuntimeException handleStatusRuntimeException(StatusRuntimeException e) {
        return e;
    }

    @GrpcExceptionHandler
    public StatusException handleStatusException(StatusException e) {
        return e;
    }
}
