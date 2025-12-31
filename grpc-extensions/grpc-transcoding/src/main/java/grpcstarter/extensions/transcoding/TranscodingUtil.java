package grpcstarter.extensions.transcoding;

import io.grpc.Status;
import org.springframework.http.HttpStatus;

/**
 * Utility class for transcoding between gRPC and HTTP.
 *
 * @author Freeman
 */
public final class TranscodingUtil {

    private TranscodingUtil() {}

    /**
     * Convert gRPC status to HTTP status.
     *
     * @param grpcStatus {@link Status}
     * @return {@link HttpStatus}
     * @see <a href="https://cloud.google.com/apis/design/errors#handling_errors">Handling Errors</a>
     */
    public static HttpStatus toHttpStatus(Status grpcStatus) {
        return switch (grpcStatus.getCode()) {
            case OK -> HttpStatus.OK;
            case CANCELLED -> HttpStatus.BAD_REQUEST; // NOTE: 499 is non-standard http/1.1 code, use 400 instead
            case UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case FAILED_PRECONDITION -> HttpStatus.BAD_REQUEST;
            case ABORTED -> HttpStatus.CONFLICT;
            case OUT_OF_RANGE -> HttpStatus.BAD_REQUEST;
            case UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DATA_LOSS -> HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
