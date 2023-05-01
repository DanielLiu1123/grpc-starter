package com.freemanan.starter.grpc.server.extension.exceptionhandling;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public class ExceptionHandlingServerInterceptor implements ServerInterceptor, Ordered {
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 1000;

    private final List<ExceptionHandler> exceptionHandlers;
    private final List<UnhandledExceptionProcessor> unhandledExceptionProcessors;

    public ExceptionHandlingServerInterceptor(
            ObjectProvider<ExceptionHandler> exceptionHandlers,
            ObjectProvider<UnhandledExceptionProcessor> unhandledExceptionProcessors) {
        this.exceptionHandlers = exceptionHandlers.orderedStream().collect(Collectors.toList());
        this.unhandledExceptionProcessors =
                unhandledExceptionProcessors.orderedStream().collect(Collectors.toList());
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return new ExceptionHandlerListener<>(
                next.startCall(call, headers), call, exceptionHandlers, unhandledExceptionProcessors);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
