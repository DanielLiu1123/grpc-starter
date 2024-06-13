package grpcstarter.extensions.transcoding;

import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author Freeman
 */
public interface ReactiveTranscoder extends RouterFunction<ServerResponse>, HandlerFunction<ServerResponse> {}
