package grpcstarter.extensions.validation;

import build.buf.protovalidate.Violation;
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.FieldPathElement;
import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
final class ValidationExceptionUtil {

    private ValidationExceptionUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

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
        for (var violation : violations) {
            badRquestBuilder.addFieldViolations(BadRequest.FieldViolation.newBuilder()
                    .setField(getFieldPath(violation))
                    .setDescription(cut(violation.toProto().getMessage()))
                    .build());
        }

        return StatusProto.toStatusRuntimeException(com.google.rpc.Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT.getNumber())
                .setMessage(message)
                .addDetails(Any.pack(badRquestBuilder.build()))
                .build());
    }

    private static String getErrorMessage(Violation violation) {
        String field = getFieldPath(violation);
        String message;
        if (StringUtils.hasText(field)) {
            message = field + ": " + violation.toProto().getMessage();
        } else {
            message = violation.toProto().getMessage();
        }
        return cut(message);
    }

    private static String getFieldPath(Violation violation) {
        return violation.toProto().getField().getElementsList().stream()
                .map(FieldPathElement::getFieldName)
                .collect(Collectors.joining("."));
    }

    private static String cut(String str) {
        return str.length() > 1000 ? str.substring(0, 1000) + "..." : str;
    }
}
