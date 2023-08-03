package com.freemanan.starter.grpc.extensions.validation;

import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public class ProtoValidateServerInterceptor implements ServerInterceptor, Ordered {

    private final Validator validator;
    private final int order;

    public ProtoValidateServerInterceptor(Validator validator, int order) {
        this.validator = validator;
        this.order = order;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {

            // Implementations are free to block for extended periods of time. Implementations are not
            // required to be thread-safe.
            private boolean aborted = false;

            @Override
            public void onMessage(ReqT message) {
                try {
                    validator.validate((Message) message);
                    super.onMessage(message);
                } catch (ValidationException ex) {
                    StatusRuntimeException status = ValidationExceptionUtil.asStatusRuntimeException(ex);
                    aborted = true;
                    call.close(status.getStatus(), status.getTrailers());
                }
            }

            @Override
            public void onHalfClose() {
                if (!aborted) {
                    super.onHalfClose();
                }
            }
        };
    }

    @Override
    public int getOrder() {
        return order;
    }
}
