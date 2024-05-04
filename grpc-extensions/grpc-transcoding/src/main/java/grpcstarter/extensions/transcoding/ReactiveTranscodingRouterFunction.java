package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.JsonUtil.canParseJson;
import static grpcstarter.extensions.transcoding.TranscodingUtil.toHttpStatus;
import static grpcstarter.extensions.transcoding.Util.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static grpcstarter.extensions.transcoding.Util.buildRequestMessage;
import static grpcstarter.extensions.transcoding.Util.getInProcessChannel;
import static grpcstarter.extensions.transcoding.Util.getReactiveRoutes;
import static grpcstarter.extensions.transcoding.Util.shutdown;
import static grpcstarter.extensions.transcoding.Util.trim;
import static io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING;
import static io.grpc.MethodDescriptor.MethodType.UNARY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import grpcstarter.extensions.transcoding.Util.Route;
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
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Freeman
 * @since 3.3.0
 */
public class ReactiveTranscodingRouterFunction
        implements RouterFunction<ServerResponse>,
                HandlerFunction<ServerResponse>,
                SmartInitializingSingleton,
                DisposableBean {

    private static final String MATCHING_ROUTE = ReactiveTranscodingRouterFunction.class.getName() + ".matchingRoute";

    private final Map<String, Route<ServerRequest>> methodNameRoutes = new HashMap<>();
    private final List<Route<ServerRequest>> routes = new ArrayList<>();
    private final HeaderConverter headerConverter;

    private Channel channel;

    public ReactiveTranscodingRouterFunction(List<BindableService> services, HeaderConverter headerConverter) {
        getReactiveRoutes(services, methodNameRoutes, routes);
        this.headerConverter = headerConverter;
    }

    @Override
    public void afterSingletonsInstantiated() {
        channel = getInProcessChannel();
    }

    @Override
    @Nonnull
    public Mono<HandlerFunction<ServerResponse>> route(@Nonnull ServerRequest request) {
        if (Objects.equals(request.method(), HttpMethod.POST)) {
            var route = methodNameRoutes.get(trim(request.path(), '/'));
            if (route != null) {
                request.attributes().put(MATCHING_ROUTE, route);
                return Mono.just(this);
            }
        }

        for (var route : routes) {
            if (route.predicate().test(request)
                    || route.additionalPredicates().stream().anyMatch(p -> p.test(request))) {
                request.attributes().put(MATCHING_ROUTE, route);
                return Mono.just(this);
            }
        }

        return Mono.empty();
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> handle(@Nonnull ServerRequest request) {
        var route = (Route<ServerRequest>) request.attributes().get(MATCHING_ROUTE);

        var methodType = route.invokeMethod().getType();

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

    private Mono<ServerResponse> processServerStreamingCall(ServerRequest request, Route<ServerRequest> route) {
        return request.bodyToMono(DataBuffer.class)
                .defaultIfEmpty(request.exchange().getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(buf -> {
                    var transcoder = getTranscoder(request, buf);
                    Message msg;
                    try {
                        msg = buildRequestMessage(transcoder, route);
                    } catch (InvalidProtocolBufferException e) {
                        return Mono.error(new ResponseStatusException(BAD_REQUEST, e.getMessage(), e));
                    }
                    // forwards http headers
                    var chan = ClientInterceptors.intercept(
                            channel,
                            MetadataUtils.newAttachHeadersInterceptor(
                                    headerConverter.toMetadata(request.headers().asHttpHeaders())));
                    var call = getCall(chan, route);
                    var response = Flux.<ServerSentEvent<String>>create(
                            sink -> ClientCalls.asyncServerStreamingCall(call, msg, new StreamObserver<>() {
                                @Override
                                public void onNext(Object o) {
                                    String json = JsonUtil.toJson(transcoder.out((Message) o, route.httpRule()));
                                    sink.next(ServerSentEvent.<String>builder()
                                            .data(json)
                                            .build());
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    if (throwable instanceof StatusRuntimeException sre) {
                                        sink.error(new TranscodingRuntimeException(
                                                toHttpStatus(sre.getStatus()), sre.getMessage(), null));
                                    } else {
                                        sink.error(throwable);
                                    }
                                }

                                @Override
                                public void onCompleted() {
                                    sink.complete();
                                }
                            }));
                    return ServerResponse.ok().body(response, ServerSentEvent.class);
                });
    }

    private static Transcoder getTranscoder(ServerRequest request, DataBuffer buf) {
        return Transcoder.create(new Transcoder.Variable(
                getBytes(buf),
                convert(request.queryParams()),
                request.exchange().getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)));
    }

    private Mono<ServerResponse> processUnaryCall(ServerRequest request, Route<ServerRequest> route) {
        return request.bodyToMono(DataBuffer.class)
                .defaultIfEmpty(request.exchange().getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(buf -> {
                    var transcoder = getTranscoder(request, buf);
                    Message msg;
                    try {
                        msg = buildRequestMessage(transcoder, route);
                    } catch (InvalidProtocolBufferException e) {
                        return Mono.error(new ResponseStatusException(BAD_REQUEST, e.getMessage(), e));
                    }
                    var headers = new AtomicReference<Metadata>();
                    var trailers = new AtomicReference<Metadata>();
                    var chan = ClientInterceptors.intercept(
                            channel,
                            MetadataUtils.newCaptureMetadataInterceptor(headers, trailers),
                            MetadataUtils.newAttachHeadersInterceptor(
                                    headerConverter.toMetadata(request.headers().asHttpHeaders())));
                    var call = getCall(chan, route);
                    return Mono.create(sink -> ClientCalls.asyncUnaryCall(call, msg, new StreamObserver<>() {
                        @Override
                        public void onNext(Object o) {
                            var builder = ServerResponse.ok().headers(h -> {
                                Metadata m = headers.get();
                                if (m != null) {
                                    h.addAll(headerConverter.toHttpHeaders(m));
                                }
                            });
                            var response = transcoder.out((Message) o, route.httpRule());
                            if (canParseJson(response)) {
                                builder.contentType(MediaType.APPLICATION_JSON);
                            }
                            builder.body(Mono.just(JsonUtil.toJson(response)), String.class)
                                    .subscribe(sink::success, sink::error);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (throwable instanceof StatusRuntimeException sre) {
                                Metadata t = trailers.get();
                                sink.error(new TranscodingRuntimeException(
                                        toHttpStatus(sre.getStatus()),
                                        sre.getMessage(),
                                        t != null ? headerConverter.toHttpHeaders(t) : null));
                            } else {
                                sink.error(throwable);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            sink.success();
                        }
                    }));
                });
    }

    private static byte[] getBytes(DataBuffer buf) {
        try (InputStream is = buf.asInputStream(true)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read DataBuffer", e);
        }
    }

    private static Map<String, String[]> convert(Map<String, List<String>> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(String[]::new)));
    }

    @Override
    public void destroy() throws Exception {
        shutdown(channel, Duration.ofSeconds(15));
    }
}
