package com.freemanan.starter.grpc.server.feature.exceptionhandling.annotation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
@Getter
public class GrpcExceptionHandlerMethod {

    private final Object bean;
    private final Integer beanOrder;
    private final Method method;
    private final Class<? extends Throwable>[] exceptions;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public GrpcExceptionHandlerMethod(Object bean, Method method) {
        this.bean = bean;
        checkMethod(method);
        this.method = method;
        this.exceptions = getExceptionsThatCanBeHandled(method);
        this.beanOrder = OrderUtils.getOrder(AopProxyUtils.ultimateTargetClass(bean));
    }

    private static void checkMethod(Method method) {
        checkMethodReturnType(method);
        checkMethodParameters(method);
    }

    private static void checkMethodReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!StatusRuntimeException.class.isAssignableFrom(returnType)
                && !StatusException.class.isAssignableFrom(returnType)
                && !Status.class.isAssignableFrom(returnType)
                && !Throwable.class.isAssignableFrom(returnType)) {
            throw new IllegalStateException(
                    "The method annotated with @GrpcExceptionHandler must return StatusRuntimeException, StatusException, Status or Throwable");
        }
    }

    private static void checkMethodParameters(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            if (!ServerCall.class.isAssignableFrom(parameterType)
                    && !Metadata.class.isAssignableFrom(parameterType)
                    && !Throwable.class.isAssignableFrom(parameterType)) {
                throw new IllegalArgumentException(
                        "Unsupported parameter type of the method annotated with @GrpcExceptionHandler: "
                                + parameterType.getName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Throwable>[] getExceptionsThatCanBeHandled(Method method) {
        GrpcExceptionHandler anno = AnnotationUtils.findAnnotation(method, GrpcExceptionHandler.class);
        Assert.notNull(anno, "The method must be annotated with @GrpcExceptionHandler");
        if (!ObjectUtils.isEmpty(anno.value())) {
            // find the first Throwable parameter,
            Optional<Parameter> parameter = Arrays.stream(method.getParameters())
                    .filter(param -> Throwable.class.isAssignableFrom(param.getType()))
                    .findFirst();
            // check if it is assignable from all the exception types
            parameter.ifPresent(value -> Arrays.stream(anno.value()).forEach(type -> {
                if (!value.getType().isAssignableFrom(type)) {
                    throw new IllegalStateException(String.format(
                            "The parameter of the method annotated with @GrpcExceptionHandler must be assignable from all the exception types, but '%s' is not assignable from '%s' on method %s",
                            value.getType().getSimpleName(), type.getSimpleName(), formatMethod(method)));
                }
            }));
            return anno.value();
        }
        List<Parameter> parameters = Arrays.stream(method.getParameters())
                .filter(param -> Throwable.class.isAssignableFrom(param.getType()))
                .collect(Collectors.toList());
        if (parameters.size() != 1) {
            throw new IllegalStateException(
                    "The method annotated with @GrpcExceptionHandler must have only one Throwable parameter: "
                            + formatMethod(method));
        }
        return new Class[] {parameters.get(0).getType()};
    }

    private static String formatMethod(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }
}
