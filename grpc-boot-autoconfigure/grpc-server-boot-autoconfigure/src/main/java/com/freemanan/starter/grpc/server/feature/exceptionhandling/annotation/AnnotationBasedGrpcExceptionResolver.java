package com.freemanan.starter.grpc.server.feature.exceptionhandling.annotation;

import com.freemanan.starter.grpc.server.feature.exceptionhandling.GrpcExceptionResolver;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
public class AnnotationBasedGrpcExceptionResolver
        implements GrpcExceptionResolver, ApplicationContextAware, SmartInitializingSingleton, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AnnotationBasedGrpcExceptionResolver.class);

    public static final int ORDER = 0;

    private final Map<Class<? extends Throwable>, GrpcExceptionHandlerMethod> exceptionClassToMethod = new HashMap<>();

    private ApplicationContext ctx;

    @Override
    public StatusRuntimeException resolve(Throwable throwable, ServerCall<?, ?> call, Metadata headers) {
        Map.Entry<Throwable, GrpcExceptionHandlerMethod> entry = findHandlerMethod(throwable);
        if (entry == null) {
            return null;
        }
        GrpcExceptionHandlerMethod method = entry.getValue();
        Throwable caughtException = entry.getKey();
        Object[] args = getArgs(method.getMethod(), caughtException, call, headers);
        Object res = ReflectionUtils.invokeMethod(method.getMethod(), method.getBean(), args);
        if (res == null) {
            log.warn(
                    "Caught exception {} but @GrpcExceptionHandler method returned null, ignoring it",
                    caughtException.getClass().getSimpleName());
            return null;
        }
        if (res instanceof StatusRuntimeException) {
            return (StatusRuntimeException) res;
        } else if (res instanceof StatusException) {
            return new StatusRuntimeException(
                    ((StatusException) res).getStatus(), ((StatusException) res).getTrailers());
        } else if (res instanceof Status) {
            return new StatusRuntimeException((Status) res);
        } else if (res instanceof Throwable) {
            Status status = Status.fromThrowable((Throwable) res);
            Metadata trailers = Status.trailersFromThrowable((Throwable) res);
            return new StatusRuntimeException(
                    status, Optional.ofNullable(trailers).orElseGet(Metadata::new));
        } else {
            throw new IllegalStateException("Unsupported return type for @GrpcExceptionHandler method: "
                    + res.getClass().getSimpleName());
        }
    }

    private static Object[] getArgs(Method method, Throwable rootCause, ServerCall<?, ?> call, Metadata headers) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            if (Throwable.class.isAssignableFrom(paramType)) {
                args[i] = rootCause;
            } else if (ServerCall.class.isAssignableFrom(paramType)) {
                args[i] = call;
            } else if (Metadata.class.isAssignableFrom(paramType)) {
                args[i] = headers;
            } else {
                log.warn("Unsupported parameter type for @GrpcExceptionHandler method: {}", paramType.getSimpleName());
            }
        }
        return args;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<GrpcExceptionHandlerMethod> methods = new ArrayList<>();
        ctx.getBeansWithAnnotation(GrpcAdvice.class)
                .forEach((beanName, bean) ->
                        ReflectionUtils.doWithMethods(AopProxyUtils.ultimateTargetClass(bean), method -> {
                            GrpcExceptionHandler anno =
                                    AnnotationUtils.findAnnotation(method, GrpcExceptionHandler.class);
                            if (anno != null) {
                                ReflectionUtils.makeAccessible(method);
                                methods.add(new GrpcExceptionHandlerMethod(bean, method));
                            }
                        }));
        Map<Class<? extends Throwable>, GrpcExceptionHandlerMethod> classToMethod = methods.stream()
                .flatMap(method -> Arrays.stream(method.getExceptions())
                        .map(exceptionClass -> new AbstractMap.SimpleEntry<>(exceptionClass, method)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> {
                    if (Objects.equals(o.getBeanOrder(), n.getBeanOrder())) {
                        throw new IllegalStateException("Duplicate exception handler method: "
                                + formatMethod(o.getMethod()) + ", " + formatMethod(n.getMethod()));
                    }
                    log.warn(
                            "Duplicate exception handler method: {}, {}. Using the one with higher priority",
                            formatMethod(o.getMethod()),
                            formatMethod(n.getMethod()));
                    if (o.getBeanOrder() == null) {
                        return n;
                    }
                    if (n.getBeanOrder() == null) {
                        return o;
                    }
                    return o.getBeanOrder() < n.getBeanOrder() ? o : n;
                }));
        exceptionClassToMethod.putAll(classToMethod);
    }

    @Nullable
    private Map.Entry<Throwable, GrpcExceptionHandlerMethod> findHandlerMethod(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            final Class<? extends Throwable> clz = current.getClass();
            GrpcExceptionHandlerMethod method = exceptionClassToMethod.keySet().stream()
                    .filter(ex -> ex.isAssignableFrom(clz))
                    .min(new ExceptionDepthComparator(clz))
                    .map(exceptionClassToMethod::get)
                    .orElse(null);
            if (method != null) {
                return new AbstractMap.SimpleEntry<>(current, method);
            }
            current = current.getCause();
        }
        return null;
    }

    private static String formatMethod(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }
}
