package com.freemanan.starter.grpc.server.extension.exceptionhandling;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * <p> This is a relatively simple exception handling implementation, which is suitable for default exception handling,
 * but users still need to manually register this class to the Spring container.
 *
 * <p> Register bean:
 * <pre>{@code
 * @Bean
 * public ExceptionHandler defaultExceptionHandler() {
 *    return new DefaultExceptionHandler();
 * }
 * }</pre>
 * <p> Import bean:
 * <pre>{@code
 * @Import(DefaultExceptionHandler.class)
 * }</pre>
 *
 * <p> NOTE: the framework provides the ability to handle exceptions, but does not do any exception handling itself.
 *
 * @author Freeman
 */
public class DefaultExceptionHandler implements ExceptionHandler {
    public static final int ORDER = LOWEST_PRECEDENCE;

    @Override
    public boolean support(Throwable throwable) {
        return (throwable instanceof RuntimeException);
    }

    @Override
    public StatusRuntimeException handle(Throwable throwable) {
        return new StatusRuntimeException(Status.INTERNAL.withDescription(throwable.getMessage()));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
