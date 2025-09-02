package grpcstarter.server;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import jakarta.annotation.Nonnull;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * @author Freeman
 */
@Data
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class GrpcRequestContext {
    static final Context.Key<GrpcRequestContext> INSTANCE = Context.key("GrpcRequestContext");

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
