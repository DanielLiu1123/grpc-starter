package com.freemanan.starter.grpc.extensions.transcoderjson.util;

import static io.grpc.MethodDescriptor.MethodType.UNARY;

import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.ServerMethodDefinition;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;

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
}
