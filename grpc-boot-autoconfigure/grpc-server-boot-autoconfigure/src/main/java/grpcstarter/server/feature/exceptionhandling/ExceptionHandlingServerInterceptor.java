package grpcstarter.server.feature.exceptionhandling;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public class ExceptionHandlingServerInterceptor implements ServerInterceptor, Ordered {
    public static final int ORDER = Ordered.LOWEST_PRECEDENCE - 1000;

    private final List<GrpcExceptionResolver> grpcExceptionResolvers;
    private final List<GrpcUnhandledExceptionProcessor> grpcUnhandledExceptionProcessors;

    public ExceptionHandlingServerInterceptor(
            ObjectProvider<GrpcExceptionResolver> exceptionHandlers,
            ObjectProvider<GrpcUnhandledExceptionProcessor> unhandledExceptionProcessors) {
        this.grpcExceptionResolvers = exceptionHandlers.orderedStream().collect(Collectors.toList());
        this.grpcUnhandledExceptionProcessors =
                unhandledExceptionProcessors.orderedStream().collect(Collectors.toList());
    }

    @Override
    public <I, O> ServerCall.Listener<I> interceptCall(
            ServerCall<I, O> call, Metadata headers, ServerCallHandler<I, O> next) {
        return new GrpcExceptionHandlerListener<>(
                next.startCall(call, headers), call, headers, grpcExceptionResolvers, grpcUnhandledExceptionProcessors);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static final class GrpcExceptionHandlerListener<I, O>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<I> {
        private final ServerCall<I, O> call;
        private final Metadata headers;
        private final List<GrpcExceptionResolver> grpcExceptionResolvers;
        private final List<GrpcUnhandledExceptionProcessor> grpcUnhandledExceptionProcessors;

        private GrpcExceptionHandlerListener(
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
                            sre.getStatus(),
                            Optional.ofNullable(sre.getTrailers()).orElseGet(Metadata::new));
                    return true;
                }
            }
            grpcUnhandledExceptionProcessors.forEach(processor -> processor.process(e, call, headers));
            return false;
        }
    }
}
