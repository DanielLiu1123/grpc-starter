package com.freemanan.starter.grpc.extensions.jsontranscoder.util;

import io.grpc.Status;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/**
 * @author Freeman
 */
@UtilityClass
public class GrpcUtil {

    /**
     * Convert gRPC status to http status.
     *
     * @param grpcStatus grpc status
     * @return http status
     * @see <a href="https://chromium.googlesource.com/external/github.com/grpc/grpc/+/refs/tags/v1.21.4-pre1/doc/statuscodes.md">statuscodes</a>
     */
    public static HttpStatus toHttpStatus(Status grpcStatus) {
        switch (grpcStatus.getCode()) {
            case OK:
                return HttpStatus.OK;
            case CANCELLED:
                return HttpStatus.BAD_REQUEST; // NOTE: 499 is non-standard http/1.1 code, use 400 instead
            case UNKNOWN:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_ARGUMENT:
                return HttpStatus.BAD_REQUEST;
            case DEADLINE_EXCEEDED:
                return HttpStatus.GATEWAY_TIMEOUT;
            case NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS:
                return HttpStatus.CONFLICT;
            case PERMISSION_DENIED:
                return HttpStatus.FORBIDDEN;
            case RESOURCE_EXHAUSTED:
                return HttpStatus.TOO_MANY_REQUESTS;
            case FAILED_PRECONDITION:
                return HttpStatus.PRECONDITION_FAILED;
            case ABORTED:
                return HttpStatus.CONFLICT;
            case OUT_OF_RANGE:
                return HttpStatus.BAD_REQUEST;
            case UNIMPLEMENTED:
                return HttpStatus.NOT_IMPLEMENTED;
            case INTERNAL:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAVAILABLE:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case DATA_LOSS:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAUTHENTICATED:
                return HttpStatus.UNAUTHORIZED;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
