package grpcstarter.extensions.transcoding;

import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Freeman
 */
public interface ServletTranscoder extends RouterFunction<ServerResponse>, HandlerFunction<ServerResponse> {}
