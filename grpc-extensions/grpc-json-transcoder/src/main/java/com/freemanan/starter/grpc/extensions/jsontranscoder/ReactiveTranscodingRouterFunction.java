package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.GrpcUtil.toHttpStatus;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.QuickRoute;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.Route;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.getInProcessChannel;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.getReactiveRoutes;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.isJson;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.toHttpHeaders;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.ConcurrentReferenceHashMap;
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
        implements RouterFunction<ServerResponse>, HandlerFunction<ServerResponse>, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(ReactiveTranscodingRouterFunction.class);

    private static final String MATCHING_ROUTE = ReactiveTranscodingRouterFunction.class.getName() + ".matchingRoute";

    private final List<ServerServiceDefinition> definitions = new ArrayList<>();
    private final List<Util.Route<ServerRequest>> routes = new ArrayList<>();
    private final Map<QuickRoute, Util.Route<ServerRequest>> fastCache = new ConcurrentReferenceHashMap<>();

    private Channel channel;

    public ReactiveTranscodingRouterFunction(List<BindableService> bindableServices) {
        bindableServices.stream().map(BindableService::bindService).forEach(definitions::add);
    }

    @Override
    public void afterSingletonsInstantiated() {
        init();
    }

    @Override
    @Nonnull
    public Mono<HandlerFunction<ServerResponse>> route(@Nonnull ServerRequest request) {
        // Check fast cache first
        var req = new Util.QuickRoute(request.method(), request.path());
        var r = fastCache.get(req);
        if (r != null) {
            request.attributes().put(MATCHING_ROUTE, r);
            return Mono.just(this);
        }

        for (Util.Route<ServerRequest> route : routes) {
            if (route.predicate().test(request)) {
                fastCache.put(req, route);
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
        var route = (Util.Route<ServerRequest>) request.attributes().get(MATCHING_ROUTE);

        MethodDescriptor.MethodType methodType = route.invokeMethod().getType();

        if (methodType == MethodDescriptor.MethodType.UNARY) {
            return processUnaryCall(request, route);
        }

        if (methodType == MethodDescriptor.MethodType.SERVER_STREAMING) {
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
                    ClientCall<Object, Object> call = getCall(channel, route);
                    Transcoder transcoder = new Transcoder(new Transcoder.Variable(
                            getBytes(buf),
                            convert(request.queryParams()),
                            request.exchange().getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)));
                    Message msg = Util.buildRequestMessage(transcoder, route);
                    Flux<ServerSentEvent<String>> response =
                            Flux.create(sink -> ClientCalls.asyncServerStreamingCall(call, msg, new StreamObserver<>() {
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
                                                toHttpStatus(sre.getStatus()), sre.getLocalizedMessage(), null));
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

    private Mono<ServerResponse> processUnaryCall(ServerRequest request, Route<ServerRequest> route) {
        return request.bodyToMono(DataBuffer.class)
                .defaultIfEmpty(request.exchange().getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(buf -> {
                    AtomicReference<Metadata> headers = new AtomicReference<>();
                    AtomicReference<Metadata> trailers = new AtomicReference<>();
                    Channel chan = ClientInterceptors.intercept(
                            channel, MetadataUtils.newCaptureMetadataInterceptor(headers, trailers));

                    ClientCall<Object, Object> call = getCall(chan, route);

                    Transcoder transcoder = new Transcoder(new Transcoder.Variable(
                            getBytes(buf),
                            convert(request.queryParams()),
                            request.exchange().getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)));
                    Message msg = Util.buildRequestMessage(transcoder, route);
                    return Mono.create(sink -> ClientCalls.asyncUnaryCall(call, msg, new StreamObserver<>() {
                        @Override
                        public void onNext(Object o) {
                            String json = JsonUtil.toJson(transcoder.out((Message) o, route.httpRule()));

                            ServerResponse.BodyBuilder builder =
                                    ServerResponse.ok().headers(h -> h.addAll(toHttpHeaders(headers.get())));
                            if (isJson(json)) {
                                builder.contentType(MediaType.APPLICATION_JSON);
                            }

                            builder.body(Mono.just(json), String.class).subscribe(sink::success, sink::error);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (throwable instanceof StatusRuntimeException sre) {
                                sink.error(new TranscodingRuntimeException(
                                        toHttpStatus(sre.getStatus()),
                                        sre.getLocalizedMessage(),
                                        toHttpHeaders(trailers.get())));
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

    private void init() {
        routes.addAll(getReactiveRoutes(definitions));

        channel = getInProcessChannel();
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
}
