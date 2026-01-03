package grpcstarter.server;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * gRPC request context.
 *
 * @author Freeman
 */
@Data
public class GrpcRequestContext {
    static final Context.Key<GrpcRequestContext> INSTANCE = Context.key("GrpcRequestContext");

    private final ServerCall<?, ?> call;

    private final Metadata headers;

    /**
     * Get {@link GrpcRequestContext} bound to current gRPC request.
     *
     * @return {@link GrpcRequestContext} bound to current gRPC request
     */
    public static @Nullable GrpcRequestContext get() {
        return INSTANCE.get();
    }
}
