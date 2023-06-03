package com.freemanan.starter.grpc.extensions.jsontranscoder.util;

import static io.grpc.MethodDescriptor.MethodType.UNARY;

import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.ServerMethodDefinition;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.NotAcceptableStatusException;

/**
 * @author Freeman
 */
@UtilityClass
public class Util {

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

    public static Map<String, HandlerMethod> getPathToMethod(ObjectProvider<BindableService> grpcServiceProvider) {
        return grpcServiceProvider.stream()
                .map(bs -> Tuple2.of(bs.bindService(), bs))
                .flatMap(en -> en.getT1().getMethods().stream()
                        .map(m -> Tuple2.of(m.getMethodDescriptor().getFullMethodName(), en.getT2())))
                .map(en -> Tuple3.of(en.getT1(), en.getT2(), findMethod(en.getT2(), en.getT1())))
                .filter(en -> en.getT3() != null)
                .map(en -> Tuple2.of("/" + en.getT1(), new HandlerMethod(en.getT2(), en.getT3())))
                .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2));
    }

    public static boolean isGrpcHandleMethod(Object handler) {
        return handler instanceof HandlerMethod && isUnaryGrpcMethod(((HandlerMethod) handler).getMethod());
    }

    /**
     * @param json json string
     * @return true if json string is a json object or json array
     */
    public static boolean isJson(String json) {
        return (json.startsWith("{") && json.endsWith("}")) || (json.startsWith("[") && json.endsWith("]"));
    }

    public static List<MediaType> getAccept(HttpHeaders headers) {
        try {
            List<MediaType> mediaTypes = headers.getAccept().stream()
                    .sorted(MediaType.SPECIFICITY_COMPARATOR)
                    .collect(Collectors.toList());
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

    public static NotAcceptableStatusException notAcceptable() {
        return new NotAcceptableStatusException("Could not find acceptable representation");
    }
}
