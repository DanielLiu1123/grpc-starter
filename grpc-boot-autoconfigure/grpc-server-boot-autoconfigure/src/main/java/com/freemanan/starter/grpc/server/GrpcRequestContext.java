package com.freemanan.starter.grpc.server;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import lombok.Data;
import org.springframework.lang.Nullable;

/**
 * @author Freeman
 */
@Data
public class GrpcRequestContext {
    public static final Context.Key<GrpcRequestContext> INSTANCE = Context.key("GrpcRequestContext");

    private final ServerCall<?, ?> call;
    private final Metadata headers;

    /**
     * Get request context bound to current gRPC request.
     *
     * @return current gRPC request context
     */
    @Nullable
    public static GrpcRequestContext get() {
        return INSTANCE.get();
    }
}
