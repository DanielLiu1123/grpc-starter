package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a relatively simple exception handling implementation, which is suitable for default exception handling.
 *
 * <p> NOTE: the framework provides the ability to handle exceptions, but does not do any exception handling itself.
 *
 * @author Freeman
 */
public class DefaultGrpcExceptionHandler implements GrpcExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultGrpcExceptionHandler.class);
    public static final int ORDER = LOWEST_PRECEDENCE;

    @Override
    public boolean support(Throwable throwable) {
        return (throwable instanceof RuntimeException);
    }

    @Override
    public StatusRuntimeException handle(Throwable throwable) {
        if (log.isWarnEnabled()) {
            log.warn(
                    "{} caught {}: ",
                    this.getClass().getSimpleName(),
                    throwable.getClass().getSimpleName(),
                    throwable);
        }
        return new StatusRuntimeException(Status.INTERNAL.withDescription(throwable.getMessage()));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
