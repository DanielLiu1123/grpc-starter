package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Freeman
 * @param <I> input/request message type
 * @param <O> output/response message type
 */
public class GrpcExceptionHandlerListener<I, O> extends SimpleForwardingServerCallListener<I> {
    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionHandlerListener.class);
    private final ServerCall<I, O> call;
    private final List<GrpcExceptionHandler> grpcExceptionHandlers;
    private final List<GrpcUnhandledExceptionProcessor> grpcUnhandledExceptionProcessors;

    protected GrpcExceptionHandlerListener(
            ServerCall.Listener<I> delegate,
            ServerCall<I, O> call,
            List<GrpcExceptionHandler> grpcExceptionHandlers,
            List<GrpcUnhandledExceptionProcessor> grpcUnhandledExceptionProcessors) {
        super(delegate);
        this.call = call;
        this.grpcExceptionHandlers = grpcExceptionHandlers;
        this.grpcUnhandledExceptionProcessors = grpcUnhandledExceptionProcessors;
    }

    @Override
    public void onMessage(I message) {
        try {
            super.onMessage(message);
        } catch (Exception e) {
            if (!handle(e)) {
                throw e;
            }
        }
    }

    @Override
    public void onHalfClose() {
        try {
            super.onHalfClose();
        } catch (Exception e) {
            if (!handle(e)) {
                throw e;
            }
        }
    }

    private boolean handle(Exception e) {
        for (GrpcExceptionHandler handler : grpcExceptionHandlers) {
            if (handler.support(e)) {
                StatusRuntimeException sre = handler.handle(e);
                if (sre == null) {
                    log.warn(
                            "Should NOT return 'null', skip ExceptionHandler: {}",
                            handler.getClass().getSimpleName());
                    continue;
                }
                if (sre.getStatus() == null) {
                    log.warn(
                            "Should NOT return 'null' status, skip ExceptionHandler: {}",
                            handler.getClass().getSimpleName());
                    continue;
                }
                if (sre.getStatus().isOk()) {
                    log.warn(
                            "Should NOT return 'OK' status while handling exception, skip ExceptionHandler: {}",
                            handler.getClass().getSimpleName());
                    continue;
                }
                call.close(
                        sre.getStatus(), Optional.ofNullable(sre.getTrailers()).orElseGet(Metadata::new));
                return true;
            }
        }
        grpcUnhandledExceptionProcessors.forEach(processor -> processor.process(e));
        return false;
    }
}
