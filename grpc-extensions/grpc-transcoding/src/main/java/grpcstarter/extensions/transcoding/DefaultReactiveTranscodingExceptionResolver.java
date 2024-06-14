package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.TranscodingUtil.toHttpStatus;

import io.grpc.StatusRuntimeException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.MonoSink;

/**
 * Default implementation of {@link ReactiveTranscodingExceptionResolver}.
 *
 * <p> This implementation just converts gRPC status to HTTP status, then throws a {@link TranscodingRuntimeException}.
 *
 * @author Freeman
 */
public class DefaultReactiveTranscodingExceptionResolver implements ReactiveTranscodingExceptionResolver {

    private final HeaderConverter headerConverter;

    public DefaultReactiveTranscodingExceptionResolver(HeaderConverter headerConverter) {
        this.headerConverter = headerConverter;
    }

    @Override
    public void resolve(MonoSink<ServerResponse> sink, StatusRuntimeException exception) {
        var trailers = exception.getTrailers();
        var e = new TranscodingRuntimeException(
                toHttpStatus(exception.getStatus()),
                exception.getMessage(),
                trailers != null ? headerConverter.toHttpHeaders(trailers) : null);

        sink.error(e);
    }
}
