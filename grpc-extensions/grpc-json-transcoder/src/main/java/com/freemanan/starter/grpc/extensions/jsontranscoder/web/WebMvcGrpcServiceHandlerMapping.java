package com.freemanan.starter.grpc.extensions.jsontranscoder.web;

import com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil;
import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.protobuf.Descriptors;
import io.grpc.BindableService;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Provide default routes for those gRPC services without custom http routes.
 *
 * <p> The default route is: POST /xx.v1.ServiceName/MethodName
 *
 * @author Freeman
 */
public class WebMvcGrpcServiceHandlerMapping extends AbstractHandlerMapping implements SmartInitializingSingleton {
    public static final int ORDER = Ordered.LOWEST_PRECEDENCE;

    /**
     * path -> HandlerMethod
     *
     * <p> path: /xx.v1.ServiceName/MethodName
     * TODO(Freeman): support /xx.v1.ServiceName.MethodName?
     */
    private final Map<String, HandlerMethod> pathToMethod = new HashMap<>();

    private final Map<HttpRule, HandlerMethod> httpRuleToMethod = new HashMap<>();

    private final List<BindableService> bindableServices = new ArrayList<>();

    public WebMvcGrpcServiceHandlerMapping(List<BindableService> bindableServices) {
        this.pathToMethod.putAll(JsonTranscoderUtil.getPathToMethod(bindableServices));
        this.bindableServices.addAll(bindableServices);
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<ServerServiceDefinition> ssds =
                bindableServices.stream().map(BindableService::bindService).collect(Collectors.toList());

        for (ServerServiceDefinition ssd : ssds) {
            Descriptors.ServiceDescriptor serviceDescriptor = getServiceDescriptor(ssd);
            if (serviceDescriptor != null) {
                Map<String, Descriptors.MethodDescriptor> methodNameToMethodDescriptor =
                        serviceDescriptor.getMethods().stream()
                                .collect(Collectors.toMap(Descriptors.MethodDescriptor::getName, Function.identity()));
                for (ServerMethodDefinition<?, ?> method : ssd.getMethods()) {
                    String methodName = method.getMethodDescriptor().getBareMethodName();
                    Descriptors.MethodDescriptor methodDescriptor = methodNameToMethodDescriptor.get(methodName);
                    if (methodDescriptor != null
                            && methodDescriptor.getOptions().hasExtension(AnnotationsProto.http)) {
                        HttpRule httpRule = methodDescriptor.getOptions().getExtension(AnnotationsProto.http);

                        HandlerMethod hm = pathToMethod.get(
                                "/" + method.getMethodDescriptor().getFullMethodName());
                        if (hm != null) {
                            httpRuleToMethod.put(httpRule, hm);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected HandlerMethod getHandlerInternal(HttpServletRequest request) {
        // Custom routes by google.api.http
        for (Map.Entry<HttpRule, HandlerMethod> entry : httpRuleToMethod.entrySet()) {
            HttpRule httpRule = entry.getKey();
            switch (httpRule.getPatternCase()) {
                case GET:
                    if (HttpMethod.GET.name().equalsIgnoreCase(request.getMethod())) {
                        String path = httpRule.getGet();
                        if (path.equals(request.getRequestURI())) {
                            return entry.getValue();
                        }
                    }
                    break;
                case POST:
                    if (HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
                        String path = httpRule.getPost();
                        if (path.equals(request.getRequestURI())) {
                            return entry.getValue();
                        }
                    }
                    break;
                case PUT:
                    if (HttpMethod.PUT.name().equalsIgnoreCase(request.getMethod())) {
                        String path = httpRule.getPut();
                        if (path.equals(request.getRequestURI())) {
                            return entry.getValue();
                        }
                    }
                    break;
                case DELETE:
                    if (HttpMethod.DELETE.name().equalsIgnoreCase(request.getMethod())) {
                        String path = httpRule.getDelete();
                        if (path.equals(request.getRequestURI())) {
                            return entry.getValue();
                        }
                    }
                    break;
                case PATCH:
                    if (HttpMethod.PATCH.name().equalsIgnoreCase(request.getMethod())) {
                        String path = httpRule.getPatch();
                        if (path.equals(request.getRequestURI())) {
                            return entry.getValue();
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        // Custom routes by @RequestMapping
        if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        for (Map.Entry<String, HandlerMethod> entry : pathToMethod.entrySet()) {
            // TODO(Freeman): ignore case ?
            if (entry.getKey().equals(request.getRequestURI())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Must after {@link RequestMappingHandlerMapping}, it will process {@link RequestMapping} for us for free!
     *
     * @see WebMvcConfigurationSupport#requestMappingHandlerMapping
     */
    @Override
    public int getOrder() {
        return ORDER;
    }

    @Nullable
    private static Descriptors.ServiceDescriptor getServiceDescriptor(ServerServiceDefinition serverServiceDefinition) {
        Object schemaDescriptor = serverServiceDefinition.getServiceDescriptor().getSchemaDescriptor();
        if (schemaDescriptor instanceof ProtoFileDescriptorSupplier protoFileDescriptorSupplier) {
            Descriptors.FileDescriptor fileDescriptor = protoFileDescriptorSupplier.getFileDescriptor();
            String serviceName = serverServiceDefinition.getServiceDescriptor().getName();
            return fileDescriptor.getServices().stream()
                    .filter(serviceDescriptor -> serviceDescriptor.getFullName().equals(serviceName))
                    .findFirst()
                    .orElseThrow();
        }
        return null;
    }
}
