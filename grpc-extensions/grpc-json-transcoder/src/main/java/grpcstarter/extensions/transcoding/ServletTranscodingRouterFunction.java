package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.TranscodingUtil.toHttpStatus;
import static grpcstarter.extensions.transcoding.Util.Route;
import static grpcstarter.extensions.transcoding.Util.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static grpcstarter.extensions.transcoding.Util.buildRequestMessage;
import static grpcstarter.extensions.transcoding.Util.getInProcessChannel;
import static grpcstarter.extensions.transcoding.Util.getServletRoutes;
import static grpcstarter.extensions.transcoding.Util.isJson;
import static grpcstarter.extensions.transcoding.Util.toHttpHeaders;
import static io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING;
import static io.grpc.MethodDescriptor.MethodType.UNARY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.util.StreamUtils.copyToByteArray;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Freeman
 * @since 3.3.0
 */
public class ServletTranscodingRouterFunction
        implements RouterFunction<ServerResponse>, HandlerFunction<ServerResponse>, SmartInitializingSingleton {

    private static final String MATCHING_ROUTE = ServletTranscodingRouterFunction.class + ".matchingRoute";

    private final List<Route<ServerRequest>> routes = new ArrayList<>();

    private Channel channel;

    public ServletTranscodingRouterFunction(List<BindableService> services) {
        routes.addAll(getServletRoutes(services));
    }

    @Override
    public void afterSingletonsInstantiated() {
        channel = getInProcessChannel();
    }

    @Override
    @Nonnull
    public Optional<HandlerFunction<ServerResponse>> route(@Nonnull ServerRequest request) {
        for (var route : routes) {
            if (route.predicate().test(request)
                    || route.additionalPredicates().stream().anyMatch(p -> p.test(request))) {
                request.attributes().put(MATCHING_ROUTE, route);
                return Optional.of(this);
            }
        }
        return Optional.empty();
    }

    /**
     * NOTE: This method can return null.
     */
    @Override
    @SuppressWarnings("unchecked")
    public ServerResponse handle(@Nonnull ServerRequest request) throws Exception {
        var route = (Util.Route<ServerRequest>) request.attributes().get(MATCHING_ROUTE);

        MethodDescriptor.MethodType methodType = route.invokeMethod().getType();

        if (methodType == UNARY) {
            return processUnaryCall(request, route);
        }

        if (methodType == SERVER_STREAMING) {
            if (!Objects.equals(request.method(), HttpMethod.GET)) {
                throw new ResponseStatusException(METHOD_NOT_ALLOWED, "SSE only supports GET method");
            }
            return processServerStreamingCall(request, route);
        }

        throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unsupported rpc method type: " + methodType);
    }

    @SuppressWarnings("unchecked")
    private static ClientCall<Object, Object> getCall(Channel channel, Route<ServerRequest> route) {
        return (ClientCall<Object, Object>) channel.newCall(route.invokeMethod(), CallOptions.DEFAULT);
    }

    @SuppressWarnings("unchecked")
    private static Transcoder getTranscoder(ServerRequest request) {
        try {
            return Transcoder.create(new Transcoder.Variable(
                    copyToByteArray(request.servletRequest().getInputStream()),
                    request.servletRequest().getParameterMap(),
                    ((Map<String, String>) request.servletRequest().getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE))));
        } catch (IOException e) {
            throw new IllegalStateException("getInputStream failed", e);
        }
    }

    private ServerResponse processUnaryCall(ServerRequest request, Route<ServerRequest> route) {
        var headers = new AtomicReference<Metadata>();
        var trailers = new AtomicReference<Metadata>();
        var transcoder = getTranscoder(request);
        var req = getMessage(route, transcoder);
        var chan =
                ClientInterceptors.intercept(channel, MetadataUtils.newCaptureMetadataInterceptor(headers, trailers));
        var call = getCall(chan, route);
        Message responseMessage;
        try {
            responseMessage = (Message) ClientCalls.blockingUnaryCall(call, req);
        } catch (StatusRuntimeException e) {
            // TODO(Freeman): Not control by problemdetails.enabled, Spring bug?
            throw new TranscodingRuntimeException(
                    toHttpStatus(e.getStatus()), e.getLocalizedMessage(), toHttpHeaders(trailers.get()));
        }

        ServerResponse.BodyBuilder builder = ServerResponse.ok().headers(h -> h.addAll(toHttpHeaders(headers.get())));
        String json = JsonUtil.toJson(transcoder.out(responseMessage, route.httpRule()));
        if (isJson(json)) {
            builder.contentType(MediaType.APPLICATION_JSON);
        }
        return builder.body(json);
    }

    private ServerResponse processServerStreamingCall(ServerRequest request, Route<ServerRequest> route) {
        var transcoder = getTranscoder(request);
        var req = getMessage(route, transcoder);
        var call = getCall(channel, route);
        return ServerResponse.sse(
                (sse -> {
                    // Cancel the call when SSE error occurs, possibly due to client disconnect
                    sse.onError(t -> call.cancel("SSE error", null));

                    ClientCalls.asyncServerStreamingCall(call, req, new StreamObserver<>() {
                        @Override
                        @SneakyThrows
                        public void onNext(Object value) {
                            String json = JsonUtil.toJson(transcoder.out((Message) value, route.httpRule()));
                            sse.data(json);
                        }

                        @Override
                        public void onError(Throwable t) {
                            if (t instanceof StatusRuntimeException sre) {
                                sse.error(new TranscodingRuntimeException(
                                        toHttpStatus(sre.getStatus()), sre.getLocalizedMessage(), null));
                            } else {
                                sse.error(t);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            sse.complete();
                        }
                    });
                }),
                Duration.ZERO);
    }

    private static Message getMessage(Route<ServerRequest> route, Transcoder transcoder) {
        try {
            return buildRequestMessage(transcoder, route);
        } catch (InvalidProtocolBufferException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getLocalizedMessage(), e);
        }
    }
}
