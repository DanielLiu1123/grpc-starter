package grpcstarter.extensions.transcoding;

import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Reactive gRPC Transcoder interface combining RouterFunction and HandlerFunction.
 *
 * @author Freeman
 */
public interface ReactiveTranscoder extends RouterFunction<ServerResponse>, HandlerFunction<ServerResponse> {}
