package grpcstarter.extensions.validation;

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
        return new StatusRuntimeException(Status.INTERNAL.withDescription(ex.getMessage()));
    }

    /**
     * Convert validation violations to {@link StatusRuntimeException}.
     *
     * @param violations validation violations
     * @return {@link StatusRuntimeException}
     */
    public static StatusRuntimeException asInvalidArgumentException(List<Violation> violations) {
        String message = violations.stream()
                .map(ValidationExceptionUtil::getErrorMessage)
                .collect(Collectors.joining(", "));
        return new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(message));
    }

    private static String getErrorMessage(Violation violation) {
        String field = violation.getFieldPath();
        if (StringUtils.hasText(field)) {
            return field + ": " + violation.getMessage();
        }
        return violation.getMessage();
    }
}
