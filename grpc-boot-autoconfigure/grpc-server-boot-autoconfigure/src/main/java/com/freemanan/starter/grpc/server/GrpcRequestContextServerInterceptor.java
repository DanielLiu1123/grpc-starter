package com.freemanan.starter.grpc.server;

import static com.freemanan.starter.grpc.server.GrpcContextKeys.ResponseMetadataModifier;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.core.PriorityOrdered;

/**
 * @author Freeman
 */
public class GrpcRequestContextServerInterceptor implements ServerInterceptor, PriorityOrdered {
    public static final Integer ORDER = 0;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context context = Context.current()
                .withValue(GrpcRequestContext.INSTANCE, new GrpcRequestContext(call, headers))
                .withValue(ResponseMetadataModifier.INSTANCE, new ResponseMetadataModifier());
        return Contexts.interceptCall(context, new ModifyResponseCall<>(call), headers, next);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static final class ModifyResponseCall<Req, Res>
            extends ForwardingServerCall.SimpleForwardingServerCall<Req, Res> {

        private ModifyResponseCall(ServerCall<Req, Res> delegate) {
            super(delegate);
        }

        @Override
        public void sendHeaders(Metadata headers) {
            setResponseMetadata(headers);
            super.sendHeaders(headers);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            setResponseMetadata(trailers);
            super.close(status, trailers);
        }

        private static void setResponseMetadata(Metadata headers) {
            ResponseMetadataModifier responseMetadataModifier = ResponseMetadataModifier.get();
            if (responseMetadataModifier != null) {
                responseMetadataModifier.getConsumers().forEach(consumer -> consumer.accept(headers));
            }
        }
    }
}
