package grpcstarter.server.feature.exceptionhandling;

import grpcstarter.server.feature.exceptionhandling.annotation.AnnotationBasedGrpcExceptionResolver;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.Nullable;

/**
 * {@link GrpcExceptionResolver} is used to resolve exceptions thrown by gRPC services.
 *
 * @author Freeman
 * @see AnnotationBasedGrpcExceptionResolver
 */
public interface GrpcExceptionResolver {

    /**
     * Resolve the given exception, return {@code null} if the exception is not handled.
     *
     * @param throwable exception to handle
     * @param call      server call
     * @param headers   headers
     * @return {@link StatusRuntimeException} or null
     */
    @Nullable
    StatusRuntimeException resolve(Throwable throwable, ServerCall<?, ?> call, Metadata headers);
}
