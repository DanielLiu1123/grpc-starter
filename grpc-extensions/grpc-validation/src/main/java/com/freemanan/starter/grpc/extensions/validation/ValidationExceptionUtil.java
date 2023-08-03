package com.freemanan.starter.grpc.extensions.validation;

import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
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
        BadRequest badRequest = BadRequest.newBuilder()
                .addFieldViolations(BadRequest.FieldViolation.newBuilder()
                        .setDescription(ex.getLocalizedMessage())
                        .build())
                .build();
        return StatusProto.toStatusRuntimeException(Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT.getNumber())
                .setMessage(ex.getLocalizedMessage())
                .addDetails(Any.pack(badRequest))
                .build());
    }
}
