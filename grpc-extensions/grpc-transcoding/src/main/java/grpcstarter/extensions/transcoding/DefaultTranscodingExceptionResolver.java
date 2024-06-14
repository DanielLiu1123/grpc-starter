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
        Metadata trailers = exception.getTrailers();

        // Spring doesn't handle ResponseStatusException thrown by MVC HandlerFunction before 6.2.0
        // see https://github.com/spring-projects/spring-framework/issues/32689
        // see https://github.com/spring-projects/spring-framework/commit/52af43d6d2ffcd1a5145b423baa3fa8a70a71ed7
        throw new TranscodingRuntimeException(
                toHttpStatus(exception.getStatus()),
                exception.getMessage(),
                trailers != null ? headerConverter.toHttpHeaders(trailers) : null);
    }
}
