package com.freemanan.starter.grpc.extensions.validation;

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
class ValidationExceptionUtil {
    /**
     * Convert {@link ValidationException} to {@link StatusRuntimeException}.
     *
     * @param ex {@link ValidationException}
     * @return {@link StatusRuntimeException}
     */
    public static StatusRuntimeException asInternalException(ValidationException ex) {
        return new StatusRuntimeException(Status.INTERNAL.withDescription(ex.getLocalizedMessage()));
    }

    /**
     * Convert validation violations to {@link StatusRuntimeException}.
     *
     * @param violations validation violations
     * @return {@link StatusRuntimeException}
     */
    public static StatusRuntimeException asInvalidArgumentException(List<Violation> violations) {
        String message = violations.stream()
                .map(e -> e.getFieldPath() + ": " + e.getMessage())
                .collect(Collectors.joining(", "));
        return new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(message));
    }
}
