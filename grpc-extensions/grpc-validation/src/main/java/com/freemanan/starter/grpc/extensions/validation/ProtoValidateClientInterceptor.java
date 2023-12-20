package com.freemanan.starter.grpc.extensions.validation;

import static com.freemanan.starter.grpc.extensions.validation.ValidationExceptionUtil.asInternalException;
import static com.freemanan.starter.grpc.extensions.validation.ValidationExceptionUtil.asInvalidArgumentException;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.MethodDescriptor;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public class ProtoValidateClientInterceptor implements ClientInterceptor, Ordered {

    private final Validator validator;
    private final int order;

    public ProtoValidateClientInterceptor(Validator validator, int order) {
        this.validator = validator;
        this.order = order;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void sendMessage(ReqT message) {
                ValidationResult result;
                try {
                    result = validator.validate((Message) message);
                } catch (ValidationException e) {
                    throw asInternalException(e);
                }
                if (result.isSuccess()) {
                    super.sendMessage(message);
                } else {
                    throw asInvalidArgumentException(result.getViolations());
                }
            }
        };
    }

    @Override
    public int getOrder() {
        return order;
    }
}
