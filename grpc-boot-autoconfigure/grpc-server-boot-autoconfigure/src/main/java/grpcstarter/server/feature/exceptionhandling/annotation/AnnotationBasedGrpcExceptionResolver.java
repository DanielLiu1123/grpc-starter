package grpcstarter.server.feature.exceptionhandling.annotation;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

import grpcstarter.server.feature.exceptionhandling.GrpcExceptionResolver;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Annotation-based gRPC exception resolver.
 *
 * @author Freeman
 */
@Slf4j
public class AnnotationBasedGrpcExceptionResolver
        implements GrpcExceptionResolver, ApplicationContextAware, SmartInitializingSingleton, Ordered, DisposableBean {

    public static final int ORDER = 0;

    /**
     * Cache exception class to {@link GrpcExceptionHandlerMethod} mapping, make it faster to find the handler method
     */
    private final ConcurrentMap<Class<? extends Throwable>, GrpcExceptionHandlerMethod> exceptionClassToMethodCache =
            new ConcurrentHashMap<>();

    private final List<GrpcAdviceBean> advices = new ArrayList<>();

    @Nullable
    private ApplicationContext ctx;

    @Override
    @Nullable
    public StatusRuntimeException resolve(Throwable throwable, ServerCall<?, ?> call, Metadata headers) {
        Map.Entry<Throwable, GrpcExceptionHandlerMethod> entry = findHandlerMethod(throwable);
        if (entry == null) {
            return null;
        }
        return handleException(entry, call, headers);
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
        populateGrpcAdviceBeans();
    }

    @Override
    public void destroy() {
        exceptionClassToMethodCache.clear();
        advices.clear();
    }

    private StatusRuntimeException handleException(
            Map.Entry<Throwable, GrpcExceptionHandlerMethod> entry, ServerCall<?, ?> call, Metadata headers) {
        GrpcExceptionHandlerMethod method = entry.getValue();
        Throwable caughtException = entry.getKey();

        Object res = invokeHandlerMethod(method, caughtException, call, headers);
        return convertResponseToStatusRuntimeException(res, method.getMethod());
    }

    @SuppressWarnings("ReturnValueIgnored")
    private void populateGrpcAdviceBeans() {
        if (ctx == null) {
            return;
        }
        List<GrpcAdviceBean> beans = new ArrayList<>();
        ctx.getBeansWithAnnotation(GrpcAdvice.class).forEach((beanName, bean) -> {
            List<GrpcExceptionHandlerMethod> beanMethods = new ArrayList<>();
            ReflectionUtils.doWithMethods(AopProxyUtils.ultimateTargetClass(bean), method -> {
                GrpcExceptionHandler anno = AnnotationUtils.findAnnotation(method, GrpcExceptionHandler.class);
                if (anno != null) {
                    ReflectionUtils.makeAccessible(method);
                    beanMethods.add(new GrpcExceptionHandlerMethod(bean, method));
                }
            });
            beans.add(new GrpcAdviceBean(bean, beanMethods));
        });
        beans.stream()
                .map(GrpcAdviceBean::getMethods)
                .flatMap(Collection::stream)
                .flatMap(method -> Arrays.stream(method.getExceptions())
                        .map(exceptionClass -> new AbstractMap.SimpleEntry<>(exceptionClass, method)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> {
                    if (Objects.equals(o.getBeanOrder(), n.getBeanOrder())) {
                        throw new IllegalStateException("Duplicate exception handler method: "
                                + formatMethod(o.getMethod()) + ", " + formatMethod(n.getMethod()));
                    }
                    GrpcExceptionHandlerMethod result;
                    if (o.getBeanOrder() == null) {
                        result = n;
                    } else if (n.getBeanOrder() == null) {
                        result = o;
                    } else {
                        result = o.getBeanOrder() < n.getBeanOrder() ? o : n;
                    }
                    log.warn(
                            "Duplicate exception handler method: {}, {}. The one with higher priority will be used: {}",
                            formatMethod(o.getMethod()),
                            formatMethod(n.getMethod()),
                            formatMethod(result.getMethod()));
                    return result;
                }));
        beans.sort(comparing(GrpcAdviceBean::getOrder, nullsLast(naturalOrder())));
        advices.addAll(beans);
    }

    private Map.@Nullable Entry<Throwable, GrpcExceptionHandlerMethod> findHandlerMethod(Throwable throwable) {
        GrpcExceptionHandlerMethod cached = exceptionClassToMethodCache.get(throwable.getClass());
        if (cached != null) {
            return new AbstractMap.SimpleEntry<>(throwable, cached);
        }
        Throwable current = throwable;
        while (current != null) {
            final Class<? extends Throwable> clz = current.getClass();
            for (GrpcAdviceBean advice : advices) {
                Map<Class<? extends Throwable>, GrpcExceptionHandlerMethod> matchedMethods = new HashMap<>();
                Optional<? extends Class<? extends Throwable>> bestMatch = advice.getMethods().stream()
                        .map(method -> Arrays.stream(method.getExceptions())
                                .filter(ex -> ex.isAssignableFrom(clz))
                                .min(new ExceptionDepthComparator(clz))
                                .map(exceptionClass -> new AbstractMap.SimpleEntry<>(exceptionClass, method))
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .peek(en -> matchedMethods.putIfAbsent(en.getKey(), en.getValue()))
                        .map(Map.Entry::getKey)
                        .min(new ExceptionDepthComparator(clz));
                if (bestMatch.isPresent()) {
                    GrpcExceptionHandlerMethod method = matchedMethods.get(bestMatch.get());
                    return new AbstractMap.SimpleEntry<>(
                            current, exceptionClassToMethodCache.computeIfAbsent(clz, k -> method));
                }
            }
            current = current.getCause();
        }
        return null;
    }

    @Nullable
    private Object invokeHandlerMethod(
            GrpcExceptionHandlerMethod method, Throwable throwable, ServerCall<?, ?> call, Metadata headers) {
        Object[] args = getArgs(method.getMethod(), throwable, call, headers);
        return ReflectionUtils.invokeMethod(method.getMethod(), method.getBean(), args);
    }

    private StatusRuntimeException convertResponseToStatusRuntimeException(@Nullable Object response, Method method) {
        if (response instanceof StatusRuntimeException sre) {
            return sre;
        }
        if (response instanceof StatusException statusException) {
            return new StatusRuntimeException(statusException.getStatus(), statusException.getTrailers());
        }
        if (response instanceof Status status) {
            return new StatusRuntimeException(status);
        }
        if (response instanceof Throwable t) {
            Status status = Status.fromThrowable(t);
            Metadata trailers = Status.trailersFromThrowable(t);
            return new StatusRuntimeException(
                    status, Optional.ofNullable(trailers).orElseGet(Metadata::new));
        }

        throw new IllegalStateException(String.format(
                "Unsupported return value (%s) for @GrpcExceptionHandler method: %s", response, formatMethod(method)));
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

    private static String formatMethod(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    private static final class GrpcAdviceBean {

        @Nullable
        private final Integer order;

        private final List<GrpcExceptionHandlerMethod> methods;

        private GrpcAdviceBean(Object bean, List<GrpcExceptionHandlerMethod> methods) {
            this.order = OrderUtils.getOrder(AopProxyUtils.ultimateTargetClass(bean));
            this.methods = methods;
        }

        @Nullable
        Integer getOrder() {
            return order;
        }

        List<GrpcExceptionHandlerMethod> getMethods() {
            return methods;
        }
    }
}
