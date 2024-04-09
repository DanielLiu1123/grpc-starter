package com.freemanan.starter.grpc.extensions.jsontranscoder.web;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.TRANSCODING_SERVER_IN_PROCESS_NAME;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.pathtemplate.PathTemplate;
import com.google.protobuf.Descriptors;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
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

    @Override
    @Nonnull
    public ServerResponse handle(@Nonnull ServerRequest request) throws Exception {
        ClientCall<Object, Object> call = getClientCall(request);
        Route route = (Route) request.attributes().get(MATCHING_ROUTE);

        call.start(
                new ClientCall.Listener<>() {
                    @Override
                    public void onMessage(Object message) {
                        super.onMessage(message);
                    }
                },
                new Metadata());

        call.request(2);

        byte[] body = request.body(byte[].class);

        return ServerResponse.status(200).body(body);
    }

    @SuppressWarnings("unchecked")
    private ClientCall<Object, Object> getClientCall(ServerRequest request) {
        Route route = (Route) request.attributes().get(MATCHING_ROUTE);
        return (ClientCall<Object, Object>) channel.newCall(route.methodDescriptor(), CallOptions.DEFAULT);
    }

    private void init() {
        for (ServerServiceDefinition ssd : definitions) {
            Descriptors.ServiceDescriptor serviceDescriptor = getServiceDescriptor(ssd);
            if (serviceDescriptor == null) continue;

            Map<String, Descriptors.MethodDescriptor> methodNameToMethodDescriptor =
                    serviceDescriptor.getMethods().stream()
                            .collect(Collectors.toMap(Descriptors.MethodDescriptor::getName, Function.identity()));
            for (ServerMethodDefinition<?, ?> serverMethodDefinition : ssd.getMethods()) {
                MethodDescriptor<?, ?> md = serverMethodDefinition.getMethodDescriptor();
                String methodName = md.getBareMethodName();
                Descriptors.MethodDescriptor methodDescriptor = methodNameToMethodDescriptor.get(methodName);
                if (methodDescriptor == null || !methodDescriptor.getOptions().hasExtension(AnnotationsProto.http)) {
                    continue;
                }

                HttpRule httpRule = methodDescriptor.getOptions().getExtension(AnnotationsProto.http);
                switch (httpRule.getPatternCase()) {
                    case GET -> routes.add(new Route(
                            md, new HttpRequestPredicate(HttpMethod.GET, PathTemplate.create(httpRule.getGet()))));
                    case PUT -> routes.add(new Route(
                            md, new HttpRequestPredicate(HttpMethod.PUT, PathTemplate.create(httpRule.getPut()))));
                    case POST -> routes.add(new Route(
                            md, new HttpRequestPredicate(HttpMethod.POST, PathTemplate.create(httpRule.getPost()))));
                    case DELETE -> routes.add(new Route(
                            md,
                            new HttpRequestPredicate(HttpMethod.DELETE, PathTemplate.create(httpRule.getDelete()))));
                    case PATCH -> routes.add(new Route(
                            md, new HttpRequestPredicate(HttpMethod.PATCH, PathTemplate.create(httpRule.getPatch()))));
                    case CUSTOM -> routes.add(new Route(
                            md,
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

    private record Route(MethodDescriptor<?, ?> methodDescriptor, HttpRequestPredicate predicate) {}

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
}
