package com.freemanan.starter.grpc.server.extension.exceptionhandling;

import io.grpc.Status;
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
     * <p> Note: return value {@link StatusRuntimeException}'s {@link Status} can't be {@link Status#OK}, otherwise it will be ignored.
     *
     * @param throwable the exception to handle
     * @return {@link StatusRuntimeException}
     */
    StatusRuntimeException handle(Throwable throwable);

    @Override
    default int getOrder() {
        return 0;
    }
}
