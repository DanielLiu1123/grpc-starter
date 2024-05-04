package grpcstarter.server.feature.exceptionhandling;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;

/**
 * @param <I> input/request message type
 * @param <O> output/response message type
 * @author Freeman
 */
public class GrpcExceptionHandlerListener<I, O> extends SimpleForwardingServerCallListener<I> {
    private final ServerCall<I, O> call;
    private final Metadata headers;
    private final List<GrpcExceptionResolver> grpcExceptionResolvers;
    private final List<GrpcUnhandledExceptionProcessor> grpcUnhandledExceptionProcessors;

    protected GrpcExceptionHandlerListener(
            ServerCall.Listener<I> delegate,
            ServerCall<I, O> call,
            Metadata headers,
            List<GrpcExceptionResolver> grpcExceptionResolvers,
            List<GrpcUnhandledExceptionProcessor> grpcUnhandledExceptionProcessors) {
        super(delegate);
        this.call = call;
        this.headers = headers;
        this.grpcExceptionResolvers = grpcExceptionResolvers;
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
        for (GrpcExceptionResolver resolver : grpcExceptionResolvers) {
            StatusRuntimeException sre = resolver.resolve(e, call, headers);
            if (sre != null) {
                call.close(
                        sre.getStatus(), Optional.ofNullable(sre.getTrailers()).orElseGet(Metadata::new));
                return true;
            }
        }
        grpcUnhandledExceptionProcessors.forEach(processor -> processor.process(e, call, headers));
        return false;
    }
}
