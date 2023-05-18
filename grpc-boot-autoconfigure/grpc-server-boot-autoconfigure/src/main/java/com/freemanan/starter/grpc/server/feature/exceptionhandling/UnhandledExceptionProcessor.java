package com.freemanan.starter.grpc.server.feature.exceptionhandling;

/**
 * Process unhandled exception.
 *
 * <p> Generally used for exception reporting.
 *
 * @author Freeman
 */
public interface UnhandledExceptionProcessor {

    /**
     * Process unhandled exception.
     *
     * @param e unhandled exception
     */
    void process(Throwable e);
}
