package com.freemanan.starter.grpc.server;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import jakarta.annotation.Nonnull;
import lombok.Data;
import org.springframework.lang.Nullable;

/**
 * @author Freeman
 */
@Data
public class GrpcRequestContext {
    public static final Context.Key<GrpcRequestContext> INSTANCE = Context.key("GrpcRequestContext");

    @Nonnull
    private final ServerCall<?, ?> call;

    @Nonnull
    private final Metadata headers;

    /**
     * Get {@link GrpcRequestContext} bound to current gRPC request.
     *
     * @return {@link GrpcRequestContext} bound to current gRPC request
     */
    @Nullable
    public static GrpcRequestContext get() {
        return INSTANCE.get();
    }
}
