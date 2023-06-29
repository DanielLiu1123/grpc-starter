package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusRuntimeException;
import org.springframework.lang.Nullable;

/**
 * @author Freeman
 */
public interface GrpcExceptionResolver {

    /**
     * Resolve the given exception, return {@code null} if not resolvable.
     *
     * @param throwable exception to handle
     * @param call      server call
     * @param headers   headers
     * @return {@link StatusRuntimeException} or null
     */
    @Nullable
    StatusRuntimeException resolve(Throwable throwable, ServerCall<?, ?> call, Metadata headers);
}
