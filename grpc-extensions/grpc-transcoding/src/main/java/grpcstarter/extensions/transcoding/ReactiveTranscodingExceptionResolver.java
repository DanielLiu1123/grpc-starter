package grpcstarter.extensions.transcoding;

import io.grpc.StatusRuntimeException;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Recover exception in Spring WebFlux when transcoding error occurs.
 *
 * <p> NOTE: only works with unary call.
 *
 * @author Freeman
 * @since 3.3.0
 */
public interface ReactiveTranscodingExceptionResolver {

    /**
     * Resolve exception thrown by transcoding.
     *
     * @param exception exception thrown by transcoding
     * @return recovered response
     */
    ServerResponse resolve(StatusRuntimeException exception);
}
