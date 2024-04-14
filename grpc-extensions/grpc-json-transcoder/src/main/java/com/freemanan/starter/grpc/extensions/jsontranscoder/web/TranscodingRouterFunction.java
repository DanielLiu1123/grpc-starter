package com.freemanan.starter.grpc.extensions.jsontranscoder.web;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.GrpcUtil.toHttpStatus;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.TRANSCODING_SERVER_IN_PROCESS_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.StreamUtils.copyToByteArray;

import com.freemanan.starter.grpc.extensions.jsontranscoder.JsonUtil;
import com.freemanan.starter.grpc.extensions.jsontranscoder.Transcoder;
import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.pathtemplate.PathTemplate;
import com.google.protobuf.DescriptorProtos;
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
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @author Freeman
 * @since 3.3.0
 */
public class TranscodingRouterFunction
        implements RouterFunction<ServerResponse>, HandlerFunction<ServerResponse>, SmartInitializingSingleton {

    private static final String MATCHING_ROUTE = TranscodingRouterFunction.class + ".matchingRoute";

    private final List<ServerServiceDefinition> definitions = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final Map<Descriptors.MethodDescriptor, Message> methodCache = new ConcurrentReferenceHashMap<>();

    private Channel channel;

    public TranscodingRouterFunction(List<BindableService> bindableServices) {
        bindableServices.stream().map(BindableService::bindService).forEach(definitions::add);
    }

    @Override
    public void afterSingletonsInstantiated() {
        init();
    }

    @Override
    @Nonnull
    public Optional<HandlerFunction<ServerResponse>> route(@Nonnull ServerRequest request) {
        for (Route route : routes) {
            if (route.predicate().test(request)) {
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
        Route route = (Route) request.attributes().get(MATCHING_ROUTE);
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
                throw new ResponseStatusException(BAD_REQUEST, "SSE only supports GET method");
            }
            return processServerStreamingCall(request, call, callMethod, route);
        }

        throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unsupported rpc method type: " + methodType);
    }

    private ServerResponse processServerStreamingCall(
            ServerRequest request,
            ClientCall<Object, Object> call,
            Descriptors.MethodDescriptor callMethod,
            Route route) {
        HttpServletRequest httpServletRequest = request.servletRequest();

        httpServletRequest.startAsync();
        AsyncContext asyncContext = httpServletRequest.getAsyncContext();
        asyncContext.setTimeout(0);

        HttpServletResponse response = getHttpServletResponse(httpServletRequest);

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

        Transcoder transcoder = getTranscoder(request);

        Message req = buildRequestMessage(transcoder, callMethod, route);

        asyncContext.start(() -> ClientCalls.asyncServerStreamingCall(call, req, new StreamObserver<>() {
            @Override
            public void onNext(Object value) {
                Object resp = transcoder.out((Message) value, route.httpRule());
                String result = JsonUtil.toJson(resp);
                try {
                    ServletOutputStream out = response.getOutputStream();
                    String ret = "data: " + result + "\n\n";
                    out.write(ret.getBytes(UTF_8));
                    out.flush();
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process response", t);
            }

            @Override
            public void onCompleted() {
                try {
                    response.getOutputStream().flush();
                    asyncContext.complete();
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to flush response", e);
                }
            }
        }));

        return null;
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
            Route route,
            AtomicReference<Metadata> grpcResponseHeaders) {
        Transcoder transcoder = getTranscoder(request);

        Message responseMessage;
        try {
            responseMessage =
                    (Message) ClientCalls.blockingUnaryCall(call, buildRequestMessage(transcoder, callMethod, route));
        } catch (StatusRuntimeException e) {
            return handleException(e);
        }

        HttpServletResponse response = getHttpServletResponse(request.servletRequest());

        toHttpHeaders(grpcResponseHeaders.get()).forEach((k, values) -> values.forEach(v -> response.addHeader(k, v)));

        write(response, JsonUtil.toJson(transcoder.out(responseMessage, route.httpRule())));

        return null;
    }

    private static HttpServletResponse getHttpServletResponse(HttpServletRequest request) {
        return Optional.of(WebAsyncUtils.getAsyncManager(request))
                .map(WebAsyncManager::getAsyncWebRequest)
                .map(e -> e.getNativeResponse(HttpServletResponse.class))
                .orElseThrow(() -> new IllegalStateException("Failed to get HttpServletResponse"));
    }

    private static void write(HttpServletResponse response, String returnVal) {
        byte[] bytes = returnVal.getBytes(UTF_8);
        response.setContentLength(bytes.length);
        if ((returnVal.startsWith("{") && returnVal.endsWith("}"))
                || (returnVal.startsWith("[") && returnVal.endsWith("]"))) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        } else {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.setCharacterEncoding(UTF_8.name());
        }

        ServletOutputStream os;
        try {
            os = response.getOutputStream();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "getWriter() method has been called on this response", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get output stream", e);
        }
        try {
            os.write(bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response", e);
        }
        try {
            os.flush();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to flush response", e);
        }
    }

    private static ServerResponse handleException(StatusRuntimeException e) {
        ServerResponse.BodyBuilder builder = ServerResponse.status(toHttpStatus(e.getStatus()));
        Optional.ofNullable(e.getTrailers())
                .map(TranscodingRouterFunction::toHttpHeaders)
                .ifPresent(grpcHeaders -> builder.headers(headers -> headers.addAll(grpcHeaders)));
        return builder.body(e.getLocalizedMessage());
    }

    private Message buildRequestMessage(Transcoder transcoder, Descriptors.MethodDescriptor callMethod, Route route) {
        Message.Builder messageBuilder = methodCache
                .computeIfAbsent(
                        callMethod,
                        k -> getDefaultMessage(route.methodDescriptor().getInputType()))
                .toBuilder();

        transcoder.into(messageBuilder, route.httpRule());

        return messageBuilder.build();
    }

    private Message getDefaultMessage(Descriptors.Descriptor descriptor) {
        DescriptorProtos.FileOptions options = descriptor.getFile().getOptions();
        String javaPackage = options.hasJavaPackage()
                ? options.getJavaPackage()
                : descriptor.getFile().getPackage();
        List<String> classNames = new ArrayList<>(2);
        if (options.getJavaMultipleFiles()) {
            classNames.add(javaPackage + "." + descriptor.getName());
        } else {
            if (options.hasJavaOuterClassname()) {
                classNames.add(javaPackage + "." + options.getJavaOuterClassname() + "$" + descriptor.getName());
            } else {
                String name = descriptor.getFile().getName(); // "google/protobuf/empty.proto"
                String fileName = name.substring(name.lastIndexOf('/') + 1); // "empty.proto"

                // If there’s a service, enum, or message (including nested types) in the file with the same name,
                // “OuterClass” will be appended to the wrapper class’s name.
                // See https://protobuf.dev/reference/java/java-generated/#invocation
                String outerClassName = snakeToPascal(fileName.replace(".proto", "")); // "Empty"
                classNames.add("%s.%sOuterClass$%s"
                        .formatted(
                                javaPackage,
                                outerClassName,
                                descriptor.getName())); // "com.google.protobuf.EmptyOuterClass$Empty"
                classNames.add("%s.%s$%s"
                        .formatted(
                                javaPackage,
                                outerClassName,
                                descriptor.getName())); // "com.google.protobuf.Empty$Empty"
            }
        }

        Class<?> clazz = null;
        for (String className : classNames) {
            try {
                clazz = ClassUtils.forName(className, null);
                break;
            } catch (ClassNotFoundException ignored) {
                // no-op
            }
        }

        if (clazz == null) {
            throw new HttpMessageConversionException("Invalid Protobuf Message type: no class found for " + classNames);
        }

        try {
            Method defaultInstance = clazz.getMethod("getDefaultInstance");
            return ((Message) defaultInstance.invoke(null));
        } catch (Exception ex) {
            throw new HttpMessageConversionException(
                    "Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
        }
    }

    private void init() {
        for (ServerServiceDefinition ssd : definitions) {
            Descriptors.ServiceDescriptor serviceDescriptor = getServiceDescriptor(ssd);
            if (serviceDescriptor == null) continue;

            Map<String, Descriptors.MethodDescriptor> methodNameToMethodDescriptor =
                    serviceDescriptor.getMethods().stream()
                            .collect(Collectors.toMap(Descriptors.MethodDescriptor::getName, Function.identity()));
            for (ServerMethodDefinition<?, ?> serverMethodDefinition : ssd.getMethods()) {
                MethodDescriptor<?, ?> invokeMethod = serverMethodDefinition.getMethodDescriptor();
                Descriptors.MethodDescriptor methodDescriptor =
                        methodNameToMethodDescriptor.get(invokeMethod.getBareMethodName());
                if (methodDescriptor == null || !methodDescriptor.getOptions().hasExtension(AnnotationsProto.http)) {
                    continue;
                }

                HttpRule httpRule = methodDescriptor.getOptions().getExtension(AnnotationsProto.http);
                switch (httpRule.getPatternCase()) {
                    case GET -> routes.add(new Route(
                            httpRule,
                            invokeMethod,
                            methodDescriptor,
                            new HttpRequestPredicate(HttpMethod.GET, PathTemplate.create(httpRule.getGet()))));
                    case PUT -> routes.add(new Route(
                            httpRule,
                            invokeMethod,
                            methodDescriptor,
                            new HttpRequestPredicate(HttpMethod.PUT, PathTemplate.create(httpRule.getPut()))));
                    case POST -> routes.add(new Route(
                            httpRule,
                            invokeMethod,
                            methodDescriptor,
                            new HttpRequestPredicate(HttpMethod.POST, PathTemplate.create(httpRule.getPost()))));
                    case DELETE -> routes.add(new Route(
                            httpRule,
                            invokeMethod,
                            methodDescriptor,
                            new HttpRequestPredicate(HttpMethod.DELETE, PathTemplate.create(httpRule.getDelete()))));
                    case PATCH -> routes.add(new Route(
                            httpRule,
                            invokeMethod,
                            methodDescriptor,
                            new HttpRequestPredicate(HttpMethod.PATCH, PathTemplate.create(httpRule.getPatch()))));
                    case CUSTOM -> routes.add(new Route(
                            httpRule,
                            invokeMethod,
                            methodDescriptor,
                            new HttpRequestPredicate(
                                    HttpMethod.valueOf(httpRule.getCustom().getKind()),
                                    PathTemplate.create(httpRule.getCustom().getPath()))));
                    case PATTERN_NOT_SET -> {
                        // no-op
                    }
                }
            }
        }

        channel = InProcessChannelBuilder.forName(TRANSCODING_SERVER_IN_PROCESS_NAME)
                .usePlaintext()
                .build();
    }

    @Nullable
    private static Descriptors.ServiceDescriptor getServiceDescriptor(ServerServiceDefinition definition) {
        Object schemaDescriptor = definition.getServiceDescriptor().getSchemaDescriptor();
        if (schemaDescriptor instanceof ProtoFileDescriptorSupplier protoFileDescriptorSupplier) {
            Descriptors.FileDescriptor fileDescriptor = protoFileDescriptorSupplier.getFileDescriptor();
            String serviceName = definition.getServiceDescriptor().getName();
            return fileDescriptor.getServices().stream()
                    .filter(serviceDescriptor -> serviceDescriptor.getFullName().equals(serviceName))
                    .findFirst()
                    .orElseThrow();
        }
        return null;
    }

    private record Route(
            HttpRule httpRule,
            MethodDescriptor<?, ?> invokeMethod,
            Descriptors.MethodDescriptor methodDescriptor,
            Predicate<ServerRequest> predicate) {}

    private record HttpRequestPredicate(HttpMethod httpMethod, PathTemplate pathTemplate)
            implements Predicate<ServerRequest> {

        @Override
        public boolean test(ServerRequest request) {
            if (!Objects.equals(request.method(), httpMethod)) return false;

            String path = request.path();
            path = StringUtils.trimLeadingCharacter(path, '/');
            path = StringUtils.trimTrailingCharacter(path, '/');
            if (path.contains(":") && !pathTemplate.endsWithCustomVerb()) {
                return false;
            }

            Map<String, String> result = pathTemplate.match(path);
            if (result == null) return false;

            Map<String, Object> attributes = request.attributes();
            attributes.put(
                    RouterFunctions.MATCHING_PATTERN_ATTRIBUTE,
                    PathPatternParser.defaultInstance.parse(
                            pathTemplate.withoutVars().toString()));
            attributes.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, result);
            return true;
        }
    }

    private static String snakeToPascal(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder result = new StringBuilder(input.length());
        boolean toUpperCase = true;

        for (char c : input.toCharArray()) {
            if (c == '_') {
                toUpperCase = true;
            } else {
                result.append(toUpperCase ? Character.toUpperCase(c) : c);
                toUpperCase = false;
            }
        }

        return result.toString();
    }

    private static HttpHeaders toHttpHeaders(Metadata headers) {
        HttpHeaders result = new HttpHeaders();
        for (String key : headers.keys()) {
            if (key.startsWith("grpc-") || key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                continue;
            }
            Iterable<String> iter = headers.getAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            if (iter != null) {
                iter.forEach(value -> result.add(key, value));
            }
        }
        return result;
    }
}
