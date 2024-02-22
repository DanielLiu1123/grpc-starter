package com.freemanan.starter.grpc.extensions.jsontranscoder.util;

import static io.grpc.MethodDescriptor.MethodType.UNARY;

import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.experimental.UtilityClass;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.NotAcceptableStatusException;

/**
 * @author Freeman
 */
@UtilityClass
public class JsonTranscoderUtil {

    public static boolean isUnaryGrpcMethod(Method method) {
        return method.getParameterCount() == 2
                && Message.class.isAssignableFrom(method.getParameterTypes()[0])
                && StreamObserver.class.isAssignableFrom(method.getParameterTypes()[1]);
    }

    private static Method findMethod(BindableService service, String fullMethodName) {
        ServerMethodDefinition<?, ?> smd = service.bindService().getMethod(fullMethodName);
        if (smd == null) {
            return null;
        }
        if (smd.getMethodDescriptor().getType() != UNARY) {
            // only support unary method !
            return null;
        }
        String methodName = smd.getMethodDescriptor().getBareMethodName();
        if (methodName == null) {
            return null;
        }
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(AopProxyUtils.ultimateTargetClass(service));
        for (Method method : methods) {
            if (isUnaryGrpcMethod(method) && method.getName().equalsIgnoreCase(methodName)) {
                return method;
            }
        }
        return null;
    }

    public static Map<String, HandlerMethod> getPathToMethod(Collection<BindableService> grpcServiceProvider) {
        return grpcServiceProvider.stream()
                .map(bindableService -> new ServiceDefinitionPair(bindableService.bindService(), bindableService))
                .flatMap(pair -> pair.getServiceDefinition().getMethods().stream()
                        .map(md -> new MethodHandlerInfo(
                                md.getMethodDescriptor().getFullMethodName(),
                                pair.getBindableService(),
                                findMethod(
                                        pair.getBindableService(),
                                        md.getMethodDescriptor().getFullMethodName()))))
                .filter(info -> info.getMethod() != null)
                .collect(Collectors.toMap(
                        info -> "/" + info.getFullMethodName(),
                        info -> new HandlerMethod(info.getBindableService(), info.getMethod())));
    }

    public static boolean isGrpcHandleMethod(Object handler) {
        return handler instanceof HandlerMethod && isUnaryGrpcMethod(((HandlerMethod) handler).getMethod());
    }

    /**
     * @param json json string
     * @return true if json string is a json object or json array
     */
    public static boolean isJson(String json) {
        if (!StringUtils.hasText(json)) {
            return false;
        }
        json = json.trim();
        return (json.startsWith("{") && json.endsWith("}")) || (json.startsWith("[") && json.endsWith("]"));
    }

    public static List<MediaType> getAccept(HttpHeaders headers) {
        try {
            List<MediaType> mediaTypes = headers.getAccept();
            MimeTypeUtils.sortBySpecificity(mediaTypes);
            return !CollectionUtils.isEmpty(mediaTypes) ? mediaTypes : Collections.singletonList(MediaType.ALL);
        } catch (InvalidMediaTypeException ex) {
            String value = headers.getFirst(HttpHeaders.ACCEPT);
            throw new NotAcceptableStatusException(
                    "Could not parse 'Accept' header [" + value + "]: " + ex.getMessage());
        }
    }

    public static boolean anyCompatible(List<MediaType> mediaTypes, MediaType otherMediaType) {
        for (MediaType mediaType : mediaTypes) {
            if (mediaType.isCompatibleWith(otherMediaType)) {
                return true;
            }
        }
        return false;
    }

    public static NotAcceptableStatusException notAcceptableException() {
        return new NotAcceptableStatusException("Could not find acceptable representation");
    }

    @Data
    private static final class ServiceDefinitionPair {
        private final ServerServiceDefinition serviceDefinition;
        private final BindableService bindableService;
    }

    @Data
    private static final class MethodHandlerInfo {
        private final String fullMethodName;
        private final BindableService bindableService;
        private final Method method;
    }
}
