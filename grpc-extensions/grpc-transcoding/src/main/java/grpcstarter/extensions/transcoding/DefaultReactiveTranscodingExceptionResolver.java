package grpcstarter.extensions.transcoding;

import io.grpc.StatusRuntimeException;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Default implementation of {@link ReactiveTranscodingExceptionResolver}.
 *
 * <p> This implementation just converts gRPC status to HTTP status, then throws a {@link TranscodingRuntimeException}.
 *
 * @author Freeman
 */
public class DefaultReactiveTranscodingExceptionResolver implements ReactiveTranscodingExceptionResolver {
    @Override
    public ServerResponse resolve(StatusRuntimeException exception) {
        // TODO(Freeman):
        return null;
    }
}
