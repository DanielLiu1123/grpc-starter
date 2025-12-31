package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.JsonUtil.canParseJson;
import static grpcstarter.extensions.transcoding.TranscodingUtil.toHttpStatus;
import static grpcstarter.extensions.transcoding.Util.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static grpcstarter.extensions.transcoding.Util.buildRequestMessage;
import static grpcstarter.extensions.transcoding.Util.getReactiveRoutes;
import static grpcstarter.extensions.transcoding.Util.getTranscodingChannel;
import static grpcstarter.extensions.transcoding.Util.shutdown;
import static grpcstarter.extensions.transcoding.Util.trimRight;
import static io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING;
import static io.grpc.MethodDescriptor.MethodType.UNARY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import grpcstarter.extensions.transcoding.Util.Route;
import grpcstarter.server.GrpcServerProperties;
import grpcstarter.server.GrpcServerStartedEvent;
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
import javax.annotation.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link ReactiveTranscoder}.
 *
 * @author Freeman
 * @since 3.3.0
 */
public class DefaultReactiveTranscoder
        implements ReactiveTranscoder, ApplicationListener<GrpcServerStartedEvent>, DisposableBean {

    private static final String MATCHING_ROUTE = DefaultReactiveTranscoder.class.getName() + ".matchingRoute";

    /**
     * grpc full method name -> route
     *
     * <p> e.g. "grpc.testing.SimpleService/UnaryRpc" -> Route
     */
    private final Map<String, Route<ServerRequest>> autoMappingRoutes = new HashMap<>();

    private final List<Route<ServerRequest>> customRoutes = new ArrayList<>();

    private final HeaderConverter headerConverter;
    private final GrpcTranscodingProperties grpcTranscodingProperties;
    private final GrpcServerProperties grpcServerProperties;
    private final ReactiveTranscodingExceptionResolver transcodingExceptionResolver;

    @Nullable
    private Channel channel;

    public DefaultReactiveTranscoder(
            List<BindableService> services,
            HeaderConverter headerConverter,
            GrpcTranscodingProperties grpcTranscodingProperties,
            GrpcServerProperties grpcServerProperties,
            ReactiveTranscodingExceptionResolver transcodingExceptionResolver,
            List<TranscodingCustomizer> transcodingCustomizers) {
        getReactiveRoutes(services, autoMappingRoutes, customRoutes, grpcTranscodingProperties, transcodingCustomizers);
        this.headerConverter = headerConverter;
        this.grpcTranscodingProperties = grpcTranscodingProperties;
        this.grpcServerProperties = grpcServerProperties;
        this.transcodingExceptionResolver = transcodingExceptionResolver;
    }

    @Override
    public void onApplicationEvent(GrpcServerStartedEvent event) {
        channel = getTranscodingChannel(event.getSource().getPort(), grpcTranscodingProperties, grpcServerProperties);
    }

    @Override
    public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
        if (Objects.equals(request.method(), HttpMethod.POST)) {
            var route = autoMappingRoutes.get(trimRight(request.path(), '/'));
            if (route != null) {
                request.attributes().put(MATCHING_ROUTE, route);
                return Mono.just(this);
            }
        }

        for (var route : customRoutes) {
            if (route.predicate().test(request)
                    || route.additionalPredicates().stream().anyMatch(p -> p.test(request))) {
                request.attributes().put(MATCHING_ROUTE, route);
                return Mono.just(this);
            }
        }

        return Mono.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> handle(ServerRequest request) {
        var route = (Route<ServerRequest>) request.attributes().get(MATCHING_ROUTE);
        if (route == null) {
            return ServerResponse.badRequest().build();
        }

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

    private Mono<ServerResponse> processServerStreamingCall(ServerRequest request, Route<ServerRequest> route) {
        return request.bodyToMono(DataBuffer.class)
                .defaultIfEmpty(request.exchange().getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(buf -> {
                    var transcoder = getTranscoder(request, buf);
                    var msg = getMessage(route, transcoder);
                    // forwards http headers
                    var chan = ClientInterceptors.intercept(
                            mustGetChannel(),
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
        var uriTemplateVariables = request.exchange().getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        @SuppressWarnings("unchecked")
        Map<String, String> templateVars =
                uriTemplateVariables != null ? (Map<String, String>) uriTemplateVariables : Map.of();
        return Transcoder.create(
                new Transcoder.Variable(getByteString(buf), convert(request.queryParams()), templateVars));
    }

    private Mono<ServerResponse> processUnaryCall(ServerRequest request, Route<ServerRequest> route) {
        return request.bodyToMono(DataBuffer.class)
                .defaultIfEmpty(request.exchange().getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(buf -> {
                    var transcoder = getTranscoder(request, buf);
                    Message msg = getMessage(route, transcoder);
                    var headers = new AtomicReference<Metadata>();
                    var trailers = new AtomicReference<Metadata>();
                    var chan = ClientInterceptors.intercept(
                            mustGetChannel(),
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
                            var body = transcoder.out((Message) o, route.httpRule());
                            if (canParseJson(body)) {
                                builder.contentType(MediaType.APPLICATION_JSON);
                            }
                            builder.body(Mono.just(JsonUtil.toJson(body)), String.class)
                                    .subscribe(sink::success, sink::error);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (throwable instanceof StatusRuntimeException sre) {
                                transcodingExceptionResolver.resolve(sink, sre);
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

    private static Message getMessage(Route<?> route, Transcoder transcoder) {
        try {
            return buildRequestMessage(transcoder, route);
        } catch (InvalidProtocolBufferException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
        }
    }

    private static ByteString getByteString(DataBuffer buf) {
        try (InputStream is = buf.asInputStream(true)) {
            return ByteString.copyFrom(is.readAllBytes());
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
        if (channel != null) {
            shutdown(channel, Duration.ofSeconds(15));
        }
    }

    Channel mustGetChannel() {
        if (channel == null) {
            throw new IllegalStateException("Channel not initialized");
        }
        return channel;
    }
}
