package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.JsonUtil.canParseJson;
import static grpcstarter.extensions.transcoding.TranscodingUtil.toHttpStatus;
import static grpcstarter.extensions.transcoding.Util.Route;
import static grpcstarter.extensions.transcoding.Util.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static grpcstarter.extensions.transcoding.Util.buildRequestMessage;
import static grpcstarter.extensions.transcoding.Util.fillRoutes;
import static grpcstarter.extensions.transcoding.Util.getTranscodingChannel;
import static grpcstarter.extensions.transcoding.Util.shutdown;
import static grpcstarter.extensions.transcoding.Util.trim;
import static io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING;
import static io.grpc.MethodDescriptor.MethodType.UNARY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.util.StreamUtils.copyToByteArray;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import grpcstarter.server.GrpcServerProperties;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.springframework.beans.factory.DisposableBean;
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
        implements RouterFunction<ServerResponse>,
                HandlerFunction<ServerResponse>,
                SmartInitializingSingleton,
                DisposableBean {

    private static final String MATCHING_ROUTE = ServletTranscodingRouterFunction.class + ".matchingRoute";

    /**
     * grpc full method name -> route
     *
     * <p> e.g. "grpc.testing.SimpleService/UnaryRpc" -> Route
     */
    private final Map<String, Route<ServerRequest>> fullMethodNameToRoute = new HashMap<>();

    private final List<Route<ServerRequest>> routes = new ArrayList<>();
    private final HeaderConverter headerConverter;
    private final GrpcTranscodingProperties properties;
    private final GrpcServerProperties grpcServerProperties;

    private Channel channel;

    public ServletTranscodingRouterFunction(
            List<BindableService> services,
            HeaderConverter headerConverter,
            GrpcTranscodingProperties properties,
            GrpcServerProperties grpcServerProperties) {
        fillRoutes(services, fullMethodNameToRoute, routes);
        this.headerConverter = headerConverter;
        this.properties = properties;
        this.grpcServerProperties = grpcServerProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        channel = getTranscodingChannel(properties, grpcServerProperties);
    }

    @Override
    @Nonnull
    public Optional<HandlerFunction<ServerResponse>> route(@Nonnull ServerRequest request) {
        if (Objects.equals(request.method(), HttpMethod.POST)) {
            var route = fullMethodNameToRoute.get(trim(request.path(), '/'));
            if (route != null) {
                request.attributes().put(MATCHING_ROUTE, route);
                return Optional.of(this);
            }
        }

        for (var route : routes) {
            if (route.predicate().test(request)
                    || route.additionalPredicates().stream().anyMatch(p -> p.test(request))) {
                request.attributes().put(MATCHING_ROUTE, route);
                return Optional.of(this);
            }
        }

        return Optional.empty();
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public ServerResponse handle(@Nonnull ServerRequest request) {
        var route = (Util.Route<ServerRequest>) request.attributes().get(MATCHING_ROUTE);

        var methodType = route.invokeMethod().getType();

        if (methodType == UNARY) {
            return processUnaryCall(request, route);
        }

        if (methodType == SERVER_STREAMING) {
            return processServerStreamingCall(request, route);
        }

        throw new ResponseStatusException(BAD_REQUEST, "Unsupported rpc method type: " + methodType);
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
        var chan = ClientInterceptors.intercept(
                channel,
                MetadataUtils.newCaptureMetadataInterceptor(headers, trailers),
                MetadataUtils.newAttachHeadersInterceptor(
                        headerConverter.toMetadata(request.headers().asHttpHeaders())));
        var call = getCall(chan, route);
        Message responseMessage;
        try {
            responseMessage = (Message) ClientCalls.blockingUnaryCall(call, req);
        } catch (StatusRuntimeException e) {
            // TODO(Freeman): Not control by problemdetails.enabled, Spring bug?
            Metadata t = trailers.get();
            throw new TranscodingRuntimeException(
                    toHttpStatus(e.getStatus()), e.getMessage(), t != null ? headerConverter.toHttpHeaders(t) : null);
        }

        var builder = ServerResponse.ok().headers(h -> {
            Metadata m = headers.get();
            if (m != null) {
                h.addAll(headerConverter.toHttpHeaders(m));
            }
        });
        var body = transcoder.out(responseMessage, route.httpRule());
        if (canParseJson(body)) {
            builder.contentType(MediaType.APPLICATION_JSON);
        }
        return builder.body(JsonUtil.toJson(body));
    }

    private ServerResponse processServerStreamingCall(ServerRequest request, Route<ServerRequest> route) {
        var transcoder = getTranscoder(request);
        var req = getMessage(route, transcoder);
        // forwards http headers
        var chan = ClientInterceptors.intercept(
                channel,
                MetadataUtils.newAttachHeadersInterceptor(
                        headerConverter.toMetadata(request.headers().asHttpHeaders())));
        var call = getCall(chan, route);
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
                                        toHttpStatus(sre.getStatus()), sre.getMessage(), null));
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

    private static Message getMessage(Route<?> route, Transcoder transcoder) {
        try {
            return buildRequestMessage(transcoder, route);
        } catch (InvalidProtocolBufferException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        shutdown(channel, Duration.ofSeconds(15));
    }
}
