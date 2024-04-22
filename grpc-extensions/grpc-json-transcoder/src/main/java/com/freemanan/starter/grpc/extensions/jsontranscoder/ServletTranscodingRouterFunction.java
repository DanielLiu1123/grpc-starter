package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.GrpcUtil.toHttpStatus;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.Route;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.getInProcessChannel;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.getServletRoutes;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.isJson;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.Util.toHttpHeaders;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.util.StreamUtils.copyToByteArray;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Freeman
 * @since 3.3.0
 */
public class ServletTranscodingRouterFunction
        implements RouterFunction<ServerResponse>, HandlerFunction<ServerResponse>, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(ServletTranscodingRouterFunction.class);

    private static final String MATCHING_ROUTE = ServletTranscodingRouterFunction.class + ".matchingRoute";

    private final List<ServerServiceDefinition> definitions = new ArrayList<>();
    private final List<Util.Route<ServerRequest>> routes = new ArrayList<>();
    private final Map<Util.QuickRoute, Util.Route<ServerRequest>> fastCache = new ConcurrentReferenceHashMap<>();

    private Channel channel;

    public ServletTranscodingRouterFunction(List<BindableService> bindableServices) {
        bindableServices.stream().map(BindableService::bindService).forEach(definitions::add);
    }

    @Override
    public void afterSingletonsInstantiated() {
        init();
    }

    @Override
    @Nonnull
    public Optional<HandlerFunction<ServerResponse>> route(@Nonnull ServerRequest request) {
        // Check fast cache first
        var req = new Util.QuickRoute(request.method(), request.path());
        var r = fastCache.get(req);
        if (r != null) {
            request.attributes().put(MATCHING_ROUTE, r);
            return Optional.of(this);
        }

        for (Util.Route<ServerRequest> route : routes) {
            if (route.predicate().test(request)) {
                fastCache.put(req, route);
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
        Util.Route<ServerRequest> route =
                (Util.Route<ServerRequest>) request.attributes().get(MATCHING_ROUTE);
        Descriptors.MethodDescriptor callMethod = route.methodDescriptor();

        ClientCall<Object, Object> call =
                (ClientCall<Object, Object>) channel.newCall(route.invokeMethod(), CallOptions.DEFAULT);

        AtomicReference<Metadata> grpcResponseHeaders = new AtomicReference<>();
        call = new ForwardingClientCall.SimpleForwardingClientCall<>(call) {
            @Override
            public void start(Listener<Object> responseListener, Metadata headers) {
                super.start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                            @Override
                            public void onHeaders(Metadata headers) {
                                grpcResponseHeaders.set(headers);
                                super.onHeaders(headers);
                            }

                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                grpcResponseHeaders.set(trailers);
                                super.onClose(status, trailers);
                            }
                        },
                        headers);
            }
        };

        MethodDescriptor.MethodType methodType = route.invokeMethod().getType();

        if (methodType == MethodDescriptor.MethodType.UNARY) {
            return processUnaryCall(request, call, callMethod, route, grpcResponseHeaders);
        }

        if (methodType == MethodDescriptor.MethodType.SERVER_STREAMING) {
            if (!Objects.equals(request.method(), HttpMethod.GET)) {
                throw new ResponseStatusException(METHOD_NOT_ALLOWED, "SSE only supports GET method");
            }
            return processServerStreamingCall(request, call, callMethod, route);
        }

        throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unsupported rpc method type: " + methodType);
    }

    private ServerResponse processServerStreamingCall(
            ServerRequest request,
            ClientCall<Object, Object> call,
            Descriptors.MethodDescriptor callMethod,
            Route<ServerRequest> route) {
        Transcoder transcoder = getTranscoder(request);

        Message req = Util.buildRequestMessage(transcoder, callMethod, route);

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

    @SuppressWarnings("unchecked")
    private static Transcoder getTranscoder(ServerRequest request) {
        try {
            return new Transcoder(new Transcoder.Variable(
                    copyToByteArray(request.servletRequest().getInputStream()),
                    request.servletRequest().getParameterMap(),
                    ((Map<String, String>)
                            request.servletRequest().getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE))));
        } catch (IOException e) {
            throw new IllegalStateException("getInputStream failed", e);
        }
    }

    private ServerResponse processUnaryCall(
            ServerRequest request,
            ClientCall<Object, Object> call,
            Descriptors.MethodDescriptor callMethod,
            Route<ServerRequest> route,
            AtomicReference<Metadata> grpcResponseHeaders) {
        Transcoder transcoder = getTranscoder(request);

        Message responseMessage;
        try {
            responseMessage = (Message)
                    ClientCalls.blockingUnaryCall(call, Util.buildRequestMessage(transcoder, callMethod, route));
        } catch (StatusRuntimeException e) {
            // Not control by problemdetails.enabled, Spring bug?
            throw new TranscodingRuntimeException(
                    toHttpStatus(e.getStatus()), e.getLocalizedMessage(), toHttpHeaders(grpcResponseHeaders.get()));
        }

        ServerResponse.BodyBuilder builder =
                ServerResponse.ok().headers(h -> h.addAll(toHttpHeaders(grpcResponseHeaders.get())));
        String json = JsonUtil.toJson(transcoder.out(responseMessage, route.httpRule()));
        if (isJson(json)) {
            builder.contentType(MediaType.APPLICATION_JSON);
        }
        return builder.body(json);
    }

    private void init() {
        routes.addAll(getServletRoutes(definitions));

        channel = getInProcessChannel();
    }
}
