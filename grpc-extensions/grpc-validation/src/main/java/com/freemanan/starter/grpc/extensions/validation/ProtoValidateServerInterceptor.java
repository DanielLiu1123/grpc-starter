package com.freemanan.starter.grpc.extensions.validation;

import static com.freemanan.starter.grpc.extensions.validation.ValidationExceptionUtil.asInvalidArgumentException;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;
import java.util.Optional;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 * @see io.envoyproxy.pgv.grpc.ValidatingServerInterceptor
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
                ValidationResult result;
                try {
                    result = validator.validate((Message) message);
                } catch (ValidationException e) {
                    aborted = true;
                    throw ValidationExceptionUtil.asInternalException(e);
                }
                if (result.isSuccess()) {
                    super.onMessage(message);
                } else {
                    StatusRuntimeException sre = asInvalidArgumentException(result.getViolations());
                    aborted = true;
                    call.close(
                            sre.getStatus(),
                            Optional.ofNullable(sre.getTrailers()).orElseGet(Metadata::new));
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
