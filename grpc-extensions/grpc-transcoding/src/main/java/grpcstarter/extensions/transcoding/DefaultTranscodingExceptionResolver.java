package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.TranscodingUtil.toHttpStatus;

import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Default implementation of {@link TranscodingExceptionResolver}.
 *
 * <p> This implementation just converts gRPC status to HTTP status.
 *
 * @author Freeman
 */
public class DefaultTranscodingExceptionResolver implements TranscodingExceptionResolver {

    private final HeaderConverter headerConverter;

    public DefaultTranscodingExceptionResolver(HeaderConverter headerConverter) {
        this.headerConverter = headerConverter;
    }

    @Override
    public ServerResponse resolve(StatusRuntimeException exception) {
        Metadata t = exception.getTrailers();
        throw new TranscodingRuntimeException(
                toHttpStatus(exception.getStatus()),
                exception.getMessage(),
                t != null ? headerConverter.toHttpHeaders(t) : null);
    }
}
