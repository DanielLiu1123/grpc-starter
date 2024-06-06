package grpcstarter.server;

import static grpcstarter.server.GrpcContextKeys.ResponseMetadataModifier;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Optional;
import org.springframework.core.PriorityOrdered;

/**
 * @author Freeman
 */
public class GrpcRequestContextServerInterceptor implements ServerInterceptor, PriorityOrdered {
    public static final Integer ORDER = 0;

    private final GrpcServerProperties grpcServerProperties;

    public GrpcRequestContextServerInterceptor(GrpcServerProperties grpcServerProperties) {
        this.grpcServerProperties = grpcServerProperties;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context context = Context.current()
                .withValue(GrpcRequestContext.INSTANCE, new GrpcRequestContext(call, headers))
                .withValue(ResponseMetadataModifier.INSTANCE, new ResponseMetadataModifier());
        Integer maxDescriptionLength = grpcServerProperties.getResponse().getMaxDescriptionLength();
        return Contexts.interceptCall(context, new ModifyResponseCall<>(call, maxDescriptionLength), headers, next);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static final class ModifyResponseCall<Req, Res>
            extends ForwardingServerCall.SimpleForwardingServerCall<Req, Res> {

        private final int maxDescriptionLength;

        private ModifyResponseCall(ServerCall<Req, Res> delegate, int maxDescriptionLength) {
            super(delegate);
            this.maxDescriptionLength = maxDescriptionLength;
        }

        @Override
        public void sendHeaders(Metadata headers) {

            setResponseMetadata(headers);

            super.sendHeaders(headers);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            Metadata trailersToUse = Optional.ofNullable(trailers).orElseGet(Metadata::new);

            setResponseMetadata(trailersToUse);

            super.close(truncateDescriptionIfNecessary(status), trailersToUse);
        }

        private Status truncateDescriptionIfNecessary(Status status) {
            String description = status.getDescription();
            if (description != null && description.length() > maxDescriptionLength) {
                return status.withDescription(String.format(
                        "%s... (%d length)", description.substring(0, maxDescriptionLength), description.length()));
            }
            return status;
        }

        private static void setResponseMetadata(Metadata headers) {
            ResponseMetadataModifier responseMetadataModifier = ResponseMetadataModifier.get();
            if (responseMetadataModifier != null) {
                responseMetadataModifier.consumers.forEach(consumer -> consumer.accept(headers));
            }
        }
    }
}
