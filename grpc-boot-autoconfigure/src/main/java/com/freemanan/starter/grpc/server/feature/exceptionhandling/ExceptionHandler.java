package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import io.grpc.StatusRuntimeException;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public interface ExceptionHandler extends Ordered {

    /**
     * Whether this handler supports the given exception.
     *
     * @param throwable the exception to handle
     * @return true if this handler supports the given exception
     */
    boolean support(Throwable throwable);

    /**
     * Process the given exception. Implementations should return a {@link StatusRuntimeException} instance.
     *
     * @param throwable the exception to handle
     * @return the status to return to the client
     */
    StatusRuntimeException handle(Throwable throwable);

    @Override
    default int getOrder() {
        return 0;
    }
}
