package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.TranscodingUtil.toHttpStatus;
import static grpcstarter.extensions.transcoding.Util.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static grpcstarter.extensions.transcoding.Util.buildRequestMessage;
import static grpcstarter.extensions.transcoding.Util.getInProcessChannel;
import static grpcstarter.extensions.transcoding.Util.getReactiveRoutes;
import static grpcstarter.extensions.transcoding.Util.isJson;
import static grpcstarter.extensions.transcoding.Util.shutdown;
import static grpcstarter.extensions.transcoding.Util.toHttpHeaders;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

import com.google.protobuf.Message;
import grpcstarter.extensions.transcoding.Util.Route;
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
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ReactiveTranscodingRouterFunction.class);

    private static final String MATCHING_ROUTE = ReactiveTranscodingRouterFunction.class.getName() + ".matchingRoute";

    private final List<Route<ServerRequest>> routes = new ArrayList<>();

    private Channel channel;

    public ReactiveTranscodingRouterFunction(List<BindableService> services) {
        routes.addAll(getReactiveRoutes(services));
    }

    @Override
    public void afterSingletonsInstantiated() {
        channel = getInProcessChannel();
    }

    @Override
    @Nonnull
    public Mono<HandlerFunction<ServerResponse>> route(@Nonnull ServerRequest request) {
        for (Route<ServerRequest> route : routes) {
            if (route.predicate().test(request)
                    || route.additionalPredicates().stream().allMatch(p -> p.test(request))) {
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
                    Transcoder transcoder = getTranscoder(request, buf);
                    Message msg = buildRequestMessage(transcoder, route);
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
                    AtomicReference<Metadata> headers = new AtomicReference<>();
                    AtomicReference<Metadata> trailers = new AtomicReference<>();
                    Channel chan = ClientInterceptors.intercept(
                            channel, MetadataUtils.newCaptureMetadataInterceptor(headers, trailers));

                    ClientCall<Object, Object> call = getCall(chan, route);

                    Transcoder transcoder = getTranscoder(request, buf);
                    Message msg = buildRequestMessage(transcoder, route);
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
