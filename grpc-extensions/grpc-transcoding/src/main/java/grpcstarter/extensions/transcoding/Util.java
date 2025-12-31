package grpcstarter.extensions.transcoding;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.pathtemplate.PathTemplate;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import grpcstarter.server.GrpcServerProperties;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * @author Freeman
 */
@UtilityClass
class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE = Util.class + ".matchingPattern";

    /**
     * Cache for the default message of the method input type.
     *
     * <p> The key is the full name of the method input type, the value is the default message instance.
     */
    static final Map<String, Message> methodCache = new ConcurrentReferenceHashMap<>();

    private static <T> List<Util.Route<T>> fillRoutes(
            Map<String, Util.Route<T>> autoMappingRoutes,
            List<Util.Route<T>> customRoutes,
            List<ServerServiceDefinition> definitions,
            BiFunction<HttpMethod, PathTemplate, Predicate<T>> predicateCreator,
            GrpcTranscodingProperties grpcTranscodingProperties,
            List<TranscodingCustomizer> transcodingCustomizers) {
        for (ServerServiceDefinition ssd : definitions) {
            Descriptors.ServiceDescriptor serviceDescriptor = Util.getServiceDescriptor(ssd);
            if (serviceDescriptor == null) {
                continue;
            }

            Map<String, Descriptors.MethodDescriptor> methodNameToMethodDescriptor =
                    serviceDescriptor.getMethods().stream()
                            .collect(Collectors.toMap(
                                    com.google.protobuf.Descriptors.MethodDescriptor::getName, Function.identity()));

            ssd.getMethods().stream()
                    .map(ServerMethodDefinition::getMethodDescriptor)
                    .forEach(invokeMethod -> {
                        var methodDescriptor = methodNameToMethodDescriptor.get(invokeMethod.getBareMethodName());
                        if (methodDescriptor == null) {
                            return;
                        }

                        boolean hasHttpExtension = methodDescriptor.getOptions().hasExtension(AnnotationsProto.http);
                        if (hasHttpExtension) {
                            Optional.ofNullable(createRouteWithBindings(
                                            invokeMethod, methodDescriptor, predicateCreator, transcodingCustomizers))
                                    .ifPresent(customRoutes::add);
                        } else if (grpcTranscodingProperties.isAutoMapping()) {
                            var httpRule = HttpRule.newBuilder()
                                    .setPost("/" + invokeMethod.getFullMethodName())
                                    .setBody("*")
                                    .build();
                            httpRule = applyCustomizers(transcodingCustomizers, httpRule, methodDescriptor);
                            if (!StringUtils.hasText(httpRule.getPost())) {
                                throw new IllegalStateException("Auto mapping requires POST method.");
                            }
                            autoMappingRoutes.put(
                                    httpRule.getPost(),
                                    new Route<>(httpRule, invokeMethod, methodDescriptor, t -> false, List.of()));
                        }
                    });
        }
        return customRoutes;
    }

    private static HttpRule applyCustomizers(
            List<TranscodingCustomizer> transcodingCustomizers,
            HttpRule httpRule,
            Descriptors.MethodDescriptor methodDescriptor) {
        var result = httpRule;
        for (var customizer : transcodingCustomizers) {
            result = customizer.customize(result, methodDescriptor);
        }
        return result;
    }

    private static <T> Util.@Nullable Route<T> createRouteWithBindings(
            MethodDescriptor<?, ?> invokeMethod,
            Descriptors.MethodDescriptor methodDescriptor,
            BiFunction<HttpMethod, PathTemplate, Predicate<T>> predicateCreator,
            List<TranscodingCustomizer> transcodingCustomizers) {
        var httpRule = methodDescriptor.getOptions().getExtension(AnnotationsProto.http);
        List<Predicate<T>> additionalPredicates = new ArrayList<>();
        // Process only one level of additional_bindings
        for (HttpRule rule : httpRule.getAdditionalBindingsList()) {
            var additionalRule = applyCustomizers(transcodingCustomizers, rule, methodDescriptor);
            HttpMethod method = extractHttpMethod(additionalRule);
            String path = extractPath(additionalRule);
            if (method != null && path != null) {
                additionalPredicates.add(predicateCreator.apply(method, PathTemplate.create(path)));
            }
        }

        var rule = applyCustomizers(transcodingCustomizers, httpRule, methodDescriptor);
        HttpMethod mainMethod = extractHttpMethod(rule);
        String mainPath = extractPath(rule);
        if (mainMethod != null && mainPath != null) {
            Predicate<T> mainPredicate = predicateCreator.apply(mainMethod, PathTemplate.create(mainPath));
            return new Route<>(rule, invokeMethod, methodDescriptor, mainPredicate, additionalPredicates);
        }
        return null;
    }

    @Nullable
    private static HttpMethod extractHttpMethod(HttpRule httpRule) {
        return switch (httpRule.getPatternCase()) {
            case GET -> HttpMethod.GET;
            case PUT -> HttpMethod.PUT;
            case POST -> HttpMethod.POST;
            case DELETE -> HttpMethod.DELETE;
            case PATCH -> HttpMethod.PATCH;
            case CUSTOM -> HttpMethod.valueOf(httpRule.getCustom().getKind());
            case PATTERN_NOT_SET -> null;
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + httpRule.getPatternCase());
        };
    }

    @Nullable
    private static String extractPath(HttpRule httpRule) {
        return switch (httpRule.getPatternCase()) {
            case GET -> httpRule.getGet();
            case PUT -> httpRule.getPut();
            case POST -> httpRule.getPost();
            case DELETE -> httpRule.getDelete();
            case PATCH -> httpRule.getPatch();
            case CUSTOM -> httpRule.getCustom().getPath();
            case PATTERN_NOT_SET -> null;
            default -> throw new IllegalArgumentException("Unsupported HTTP pattern: " + httpRule.getPatternCase());
        };
    }

    public static List<ServerServiceDefinition> listDefinition(List<BindableService> services) {
        return services.stream().map(BindableService::bindService).toList();
    }

    public static List<Util.Route<ServerRequest>> fillRoutes(
            List<BindableService> services,
            Map<String, Route<ServerRequest>> autoMappingRoutes,
            List<Util.Route<ServerRequest>> customRoutes,
            GrpcTranscodingProperties grpcTranscodingProperties,
            List<TranscodingCustomizer> transcodingCustomizers) {
        return fillRoutes(
                autoMappingRoutes,
                customRoutes,
                listDefinition(services),
                ServletPredicate::new,
                grpcTranscodingProperties,
                transcodingCustomizers);
    }

    public static List<Util.Route<org.springframework.web.reactive.function.server.ServerRequest>> getReactiveRoutes(
            List<BindableService> services,
            Map<String, Route<org.springframework.web.reactive.function.server.ServerRequest>> autoMappingRoutes,
            List<Util.Route<org.springframework.web.reactive.function.server.ServerRequest>> customRoutes,
            GrpcTranscodingProperties grpcTranscodingProperties,
            List<TranscodingCustomizer> transcodingCustomizers) {
        return fillRoutes(
                autoMappingRoutes,
                customRoutes,
                listDefinition(services),
                ReactivePredicate::new,
                grpcTranscodingProperties,
                transcodingCustomizers);
    }

    static String snakeToPascal(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder result = new StringBuilder(input.length());
        boolean toUpperCase = true;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_') {
                toUpperCase = true;
            } else {
                result.append(toUpperCase ? Character.toUpperCase(c) : c);
                toUpperCase = false;
            }
        }

        return result.toString();
    }

    private static Descriptors.@Nullable ServiceDescriptor getServiceDescriptor(ServerServiceDefinition definition) {
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

    static Message getDefaultMessage(Descriptors.Descriptor descriptor) {
        Class<?> javaClass = ProtobufJavaTypeUtil.findJavaClass(descriptor);
        try {
            Method defaultInstance = javaClass.getMethod("getDefaultInstance");
            return ((Message) defaultInstance.invoke(null));
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Invalid Protobuf Message type: no invocable getDefaultInstance() method on " + javaClass.getName(),
                    ex);
        }
    }

    public static String getClassName(Descriptors.Descriptor descriptor) {
        String className = "";
        while (descriptor != null) {
            className = descriptor.getName() + (StringUtils.hasText(className) ? "$" + className : "");
            descriptor = descriptor.getContainingType();
        }
        return className;
    }

    public static Channel getTranscodingChannel(
            int port, GrpcTranscodingProperties grpcTranscodingProperties, GrpcServerProperties grpcServerProperties) {
        var inProcess = grpcServerProperties.getInProcess();
        if (inProcess != null && StringUtils.hasText(inProcess.name())) {
            var builder = InProcessChannelBuilder.forName(inProcess.name());
            populateChannel(builder, grpcServerProperties);
            return builder.build();
        }

        String endpoint = StringUtils.hasText(grpcTranscodingProperties.getEndpoint())
                ? grpcTranscodingProperties.getEndpoint()
                : "localhost:" + port;
        var builder = ManagedChannelBuilder.forTarget(endpoint);
        populateChannel(builder, grpcServerProperties);
        if (!StringUtils.hasText(grpcServerProperties.getSslBundle())) {
            builder.usePlaintext();
        }
        return builder.build();
    }

    private static ManagedChannelBuilder<? extends ManagedChannelBuilder<?>> populateChannel(
            ManagedChannelBuilder<? extends ManagedChannelBuilder<?>> channelBuilder,
            GrpcServerProperties grpcServerProperties) {

        Optional.ofNullable(grpcServerProperties.getMaxInboundMessageSize())
                .map(DataSize::toBytes)
                .map(Long::intValue)
                .ifPresent(channelBuilder::maxInboundMessageSize);
        Optional.ofNullable(grpcServerProperties.getMaxInboundMetadataSize())
                .map(DataSize::toBytes)
                .map(Long::intValue)
                .ifPresent(channelBuilder::maxInboundMetadataSize);

        return channelBuilder;
    }

    public static Message buildRequestMessage(Transcoder transcoder, Route<?> route)
            throws InvalidProtocolBufferException {
        Message.Builder messageBuilder = methodCache
                .computeIfAbsent(
                        route.methodDescriptor().getInputType().getFullName(),
                        k -> getDefaultMessage(route.methodDescriptor().getInputType()))
                .toBuilder();

        transcoder.into(messageBuilder, route.httpRule());

        return messageBuilder.build();
    }

    public static void shutdown(Channel channel, Duration timeout) {
        if (!(channel instanceof ManagedChannel mc) || mc.isShutdown() || mc.isTerminated()) {
            return;
        }

        long ms = timeout.toMillis();
        // Close the gRPC managed-channel if not shut down already.
        try {
            mc.shutdown();
            if (!mc.awaitTermination(ms, TimeUnit.MILLISECONDS)) {
                log.warn("Graceful shutdown timed out: {}ms, channel: {}", ms, mc);
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted gracefully shutting down channel: {}", mc);
            Thread.currentThread().interrupt();
        }

        // Forcefully shut down if still not terminated.
        if (!mc.isTerminated()) {
            try {
                mc.shutdownNow();
                if (!mc.awaitTermination(15, TimeUnit.SECONDS)) {
                    log.warn("Forcefully shutdown timed out: 15s, channel: {}. ", mc);
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted forcefully shutting down channel: {}. ", mc);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Check if the protobuf message is a simple value.
     *
     * @param message protobuf message
     * @return true if the message is simple value
     */
    public static boolean isSimpleValueMessage(Message message) {
        if (isWrapperType(message.getClass())) {
            return true;
        }
        if (message instanceof Value value) {
            Value.KindCase kind = value.getKindCase();
            return kind == Value.KindCase.NULL_VALUE
                    || kind == Value.KindCase.NUMBER_VALUE
                    || kind == Value.KindCase.STRING_VALUE
                    || kind == Value.KindCase.BOOL_VALUE;
        }
        return false;
    }

    private static boolean isWrapperType(Class<?> clz) {
        return BoolValue.class.isAssignableFrom(clz)
                || Int32Value.class.isAssignableFrom(clz)
                || Int64Value.class.isAssignableFrom(clz)
                || UInt32Value.class.isAssignableFrom(clz)
                || UInt64Value.class.isAssignableFrom(clz)
                || FloatValue.class.isAssignableFrom(clz)
                || DoubleValue.class.isAssignableFrom(clz)
                || StringValue.class.isAssignableFrom(clz)
                || BytesValue.class.isAssignableFrom(clz);
    }

    private static boolean isMatch(
            HttpMethod requestMethod,
            HttpMethod httpMethod,
            String path,
            PathTemplate pathTemplate,
            Map<String, Object> attributes) {
        if (!Objects.equals(requestMethod, httpMethod)) return false;

        path = trim(path, '/');
        if (path.contains(":") && !pathTemplate.endsWithCustomVerb()) {
            return false;
        }

        Map<String, String> result = pathTemplate.match(path);
        if (result == null) {
            return false;
        }

        attributes.put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, result);
        return true;
    }

    static String trimRight(String str, char c) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        int end = str.length();
        while (end > 0 && str.charAt(end - 1) == c) {
            end--;
        }
        return str.substring(0, end);
    }

    static String trim(String str, char c) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        int start = 0;
        int end = str.length();
        while (start < end && str.charAt(start) == c) {
            start++;
        }
        while (end > start && str.charAt(end - 1) == c) {
            end--;
        }
        return str.substring(start, end);
    }

    record Route<T>(
            HttpRule httpRule,
            MethodDescriptor<?, ?> invokeMethod,
            Descriptors.MethodDescriptor methodDescriptor,
            Predicate<T> predicate,
            List<Predicate<T>> additionalPredicates) {}

    record ServletPredicate(HttpMethod httpMethod, PathTemplate pathTemplate) implements Predicate<ServerRequest> {

        @Override
        public boolean test(ServerRequest request) {
            return isMatch(request.method(), httpMethod, request.path(), pathTemplate, request.attributes());
        }
    }

    record ReactivePredicate(HttpMethod httpMethod, PathTemplate pathTemplate)
            implements Predicate<org.springframework.web.reactive.function.server.ServerRequest> {

        @Override
        public boolean test(org.springframework.web.reactive.function.server.ServerRequest request) {
            return isMatch(request.method(), httpMethod, request.path(), pathTemplate, request.attributes());
        }
    }
}
