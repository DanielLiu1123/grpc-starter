package grpcstarter.extensions.validation;

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

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
        return new StatusRuntimeException(
                Status.INTERNAL.withDescription(ex.getMessage()).withCause(ex));
    }

    /**
     * Convert validation violations to {@link StatusRuntimeException}.
     *
     * @param violations validation violations
     * @return {@link StatusRuntimeException}
     */
    public static StatusRuntimeException asInvalidArgumentException(List<Violation> violations) {
        var message = violations.stream()
                .map(ValidationExceptionUtil::getErrorMessage)
                .collect(Collectors.joining(", "));

        var badRquestBuilder = BadRequest.newBuilder();
        for (Violation violation : violations) {
            badRquestBuilder.addFieldViolations(BadRequest.FieldViolation.newBuilder()
                    .setField(violation.getFieldPath())
                    .setDescription(cut(violation.getMessage()))
                    .build());
        }

        return StatusProto.toStatusRuntimeException(com.google.rpc.Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT.getNumber())
                .setMessage(message)
                .addDetails(Any.pack(badRquestBuilder.build()))
                .build());
    }

    private static String getErrorMessage(Violation violation) {
        String field = violation.getFieldPath();
        String message;
        if (StringUtils.hasText(field)) {
            message = field + ": " + violation.getMessage();
        } else {
            message = violation.getMessage();
        }
        return cut(message);
    }

    private static String cut(String str) {
        return str.length() > 1000 ? str.substring(0, 1000) + "..." : str;
    }
}
