package grpcstarter.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Deadline;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * {@link GrpcClientOptions} is used to set options for gRPC stubs,
 * those options belong to single stub rather than a {@link Channel}.
 *
 * @author Freeman
 * @see io.grpc.stub.AbstractStub#withDeadline(Deadline)
 * @see io.grpc.stub.AbstractStub#withMaxOutboundMessageSize(int)
 * @see io.grpc.stub.AbstractStub#withCompression(String)
 * @since 3.2.0
 */
@Data
public class GrpcClientOptions {

    public static final CallOptions.Key<GrpcClientOptions> KEY =
            CallOptions.Key.createWithDefault("grpcClientOptions", new GrpcClientOptions());

    private @Nullable Long deadline;

    private @Nullable Integer maxOutboundMessageSize;

    private @Nullable String compression;
}
