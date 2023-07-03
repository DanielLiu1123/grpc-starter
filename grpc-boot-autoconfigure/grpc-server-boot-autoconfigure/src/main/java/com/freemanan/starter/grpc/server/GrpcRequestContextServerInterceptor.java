package com.freemanan.starter.grpc.server;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.springframework.core.PriorityOrdered;

/**
 * @author Freeman
 */
public class GrpcRequestContextServerInterceptor implements ServerInterceptor, PriorityOrdered {
    public static final Integer ORDER = 0;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        GrpcRequestContext ctx = new GrpcRequestContext(call, headers);
        return Contexts.interceptCall(
                Context.current().withValue(GrpcRequestContext.INSTANCE, ctx), call, headers, next);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
