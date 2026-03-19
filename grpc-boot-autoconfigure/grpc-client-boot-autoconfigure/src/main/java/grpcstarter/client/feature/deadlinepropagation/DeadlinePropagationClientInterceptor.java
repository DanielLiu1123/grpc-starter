package grpcstarter.client.feature.deadlinepropagation;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.MethodDescriptor;
import org.springframework.core.Ordered;

/**
 * A {@link ClientInterceptor} that propagates the upstream gRPC deadline to outgoing client calls.
 *
 * <p>When a gRPC server receives a request with a deadline, the gRPC framework automatically
 * sets {@link Context#getDeadline()} for the duration of that RPC. This interceptor picks up
 * that deadline and applies it to any outgoing client calls made during the same context,
 * preventing cascading timeouts in downstream services.
 *
 * <p>Only sets the deadline if the call options do not already have an explicit deadline,
 * so per-stub deadlines always take precedence.
 *
 * @author Freeman
 * @see Context#getDeadline()
 */
public class DeadlinePropagationClientInterceptor implements ClientInterceptor, Ordered {

    /**
     * Order is {@code LOWEST_PRECEDENCE - 1} so this interceptor runs inside
     * {@code GrpcClientOptionsClientInterceptor} (which has no {@link Ordered} and defaults to
     * {@code LOWEST_PRECEDENCE}). This ensures explicit per-stub deadlines are set first,
     * and this interceptor only fills in if none is present.
     */
    public static final int ORDER = Ordered.LOWEST_PRECEDENCE - 1;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        if (callOptions.getDeadline() == null) {
            Deadline contextDeadline = Context.current().getDeadline();
            if (contextDeadline != null) {
                callOptions = callOptions.withDeadline(contextDeadline);
            }
        }
        return next.newCall(method, callOptions);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
