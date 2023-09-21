package com.freemanan.starter.grpc.extensions.validation;

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
public class ValidationExceptionUtil {
    /**
     * Convert {@link ValidationException} to {@link StatusRuntimeException}
     *
     * @param ex {@link ValidationException}
     * @return {@link StatusRuntimeException}
     */
    public static StatusRuntimeException asStatusRuntimeException(ValidationException ex) {
        return StatusProto.toStatusRuntimeException(Status.newBuilder()
                .setCode(Code.INTERNAL.getNumber())
                .setMessage(ex.getLocalizedMessage())
                .build());
    }

    public static StatusRuntimeException asStatusRuntimeException(List<Violation> violations) {
        return StatusProto.toStatusRuntimeException(Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT.getNumber())
                .setMessage(violations.get(0).getMessage())
                .build());
    }
}
