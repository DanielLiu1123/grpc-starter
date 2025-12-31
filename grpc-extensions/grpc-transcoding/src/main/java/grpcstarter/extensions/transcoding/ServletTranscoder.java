package grpcstarter.extensions.transcoding;

import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * gRPC transcoder for Servlet-based applications.
 *
 * @author Freeman
 */
public interface ServletTranscoder extends RouterFunction<ServerResponse>, HandlerFunction<ServerResponse> {}
