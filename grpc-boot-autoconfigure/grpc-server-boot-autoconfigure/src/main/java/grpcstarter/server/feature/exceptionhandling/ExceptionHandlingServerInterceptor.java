package grpcstarter.server.feature.exceptionhandling;

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
}
