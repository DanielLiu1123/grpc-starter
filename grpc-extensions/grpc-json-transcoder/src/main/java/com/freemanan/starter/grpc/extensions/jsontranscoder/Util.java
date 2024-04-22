package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.JsonTranscoderUtil.TRANSCODING_SERVER_IN_PROCESS_NAME;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.pathtemplate.PathTemplate;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import jakarta.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * @author Freeman
 */
class Util {

    static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE = Util.class + ".matchingPattern";

    static final Map<Descriptors.MethodDescriptor, Message> methodCache = new ConcurrentReferenceHashMap<>();

    private static <T> List<Util.Route<T>> getRoutes(
            List<ServerServiceDefinition> definitions,
            BiFunction<HttpMethod, PathTemplate, Predicate<T>> predicateCreator) {
        List<Util.Route<T>> routes = new ArrayList<>();
        for (ServerServiceDefinition ssd : definitions) {
            Descriptors.ServiceDescriptor serviceDescriptor = Util.getServiceDescriptor(ssd);
            if (serviceDescriptor == null) continue;

            Map<String, Descriptors.MethodDescriptor> methodNameToMethodDescriptor =
                    serviceDescriptor.getMethods().stream()
                            .collect(Collectors.toMap(
                                    com.google.protobuf.Descriptors.MethodDescriptor::getName, Function.identity()));

            for (ServerMethodDefinition<?, ?> serverMethodDefinition : ssd.getMethods()) {
                MethodDescriptor<?, ?> invokeMethod = serverMethodDefinition.getMethodDescriptor();
                Descriptors.MethodDescriptor methodDescriptor =
                        methodNameToMethodDescriptor.get(invokeMethod.getBareMethodName());

                if (methodDescriptor == null || !methodDescriptor.getOptions().hasExtension(AnnotationsProto.http)) {
                    continue;
                }

                HttpRule httpRule = methodDescriptor.getOptions().getExtension(AnnotationsProto.http);
                Optional.ofNullable(createRouteWithBindings(httpRule, invokeMethod, methodDescriptor, predicateCreator))
                        .ifPresent(routes::add);
            }
        }
        return routes;
    }

    @Nullable
    private static <T> Util.Route<T> createRouteWithBindings(
            HttpRule httpRule,
            MethodDescriptor<?, ?> invokeMethod,
            Descriptors.MethodDescriptor methodDescriptor,
            BiFunction<HttpMethod, PathTemplate, Predicate<T>> predicateCreator) {
        List<Predicate<T>> additionalPredicates = new ArrayList<>();
        // Process only one level of additional_bindings
        for (HttpRule binding : httpRule.getAdditionalBindingsList()) {
            HttpMethod method = extractHttpMethod(binding);
            String path = extractPath(binding);
            if (method != null && path != null) {
                additionalPredicates.add(predicateCreator.apply(method, PathTemplate.create(path)));
            }
        }

        HttpMethod mainMethod = extractHttpMethod(httpRule);
        String mainPath = extractPath(httpRule);
        if (mainMethod != null && mainPath != null) {
            Predicate<T> mainPredicate = predicateCreator.apply(mainMethod, PathTemplate.create(mainPath));
            return new Route<>(httpRule, invokeMethod, methodDescriptor, mainPredicate, additionalPredicates);
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

    static List<Util.Route<ServerRequest>> getServletRoutes(List<ServerServiceDefinition> definitions) {
        return getRoutes(definitions, ServletPredicate::new);
    }

    static List<Util.Route<org.springframework.web.reactive.function.server.ServerRequest>> getReactiveRoutes(
            List<ServerServiceDefinition> definitions) {
        return getRoutes(definitions, ReactivePredicate::new);
    }

    static String snakeToPascal(String input) {
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

    static HttpHeaders toHttpHeaders(@Nullable Metadata headers) {
        if (headers == null) return new HttpHeaders();

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

    private static Message getDefaultMessage(Descriptors.Descriptor descriptor) {
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
            throw new IllegalStateException("Unable to find Protobuf Message type: " + classNames);
        }

        try {
            Method defaultInstance = clazz.getMethod("getDefaultInstance");
            return ((Message) defaultInstance.invoke(null));
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
        }
    }

    public static ManagedChannel getInProcessChannel() {
        return InProcessChannelBuilder.forName(TRANSCODING_SERVER_IN_PROCESS_NAME)
                .usePlaintext()
                .build();
    }

    public static Message buildRequestMessage(Transcoder transcoder, Route<?> route) {
        Message.Builder messageBuilder = methodCache
                .computeIfAbsent(
                        route.methodDescriptor(),
                        k -> getDefaultMessage(route.methodDescriptor().getInputType()))
                .toBuilder();

        transcoder.into(messageBuilder, route.httpRule());

        return messageBuilder.build();
    }

    public static boolean isJson(String string) {
        return string != null
                && ((string.startsWith("{") && string.endsWith("}"))
                        || (string.startsWith("[") && string.endsWith("]")));
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

    private static boolean isMatch(
            HttpMethod requestMethod,
            HttpMethod httpMethod,
            String path,
            PathTemplate pathTemplate,
            Map<String, Object> attributes) {
        if (!Objects.equals(requestMethod, httpMethod)) return false;

        path = StringUtils.trimLeadingCharacter(path, '/');
        path = StringUtils.trimTrailingCharacter(path, '/');
        if (path.contains(":") && !pathTemplate.endsWithCustomVerb()) {
            return false;
        }

        Map<String, String> result = pathTemplate.match(path);
        if (result == null) return false;

        attributes.put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, result);
        return true;
    }

    record QuickRoute(HttpMethod method, String path) {}
}
