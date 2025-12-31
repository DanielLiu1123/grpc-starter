package grpcstarter.extensions.test;

import grpcstarter.server.GrpcServerProperties;
import grpcstarter.server.GrpcServerStartedEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
class GrpcTestBeanPostProcessor
        implements ApplicationListener<GrpcServerStartedEvent>, BeanPostProcessor, BeanFactoryAware, DisposableBean {

    private final IdentityHashMap<Object, Boolean> beanToInjected = new IdentityHashMap<>();

    @Nullable
    private BeanFactory beanFactory;

    private int port;

    @Nullable
    private String inProcessName;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        boolean isTestInstance = isTestInstance(bean);
        if (isTestInstance || hasRelevantAnnotations(bean)) {
            beanToInjected.put(bean, false);
            // This bean is test class instance, test class instance inject fields after Spring context initialization
            // see org.springframework.test.context.support.DependencyInjectionTestExecutionListener#injectDependencies
            if (isTestInstance) {
                injectFields(bean);
            }
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(GrpcServerStartedEvent event) {
        this.port = event.getSource().getPort();
        if (beanFactory != null) {
            this.inProcessName = Optional.ofNullable(
                            beanFactory.getBean(GrpcServerProperties.class).getInProcess())
                    .map(GrpcServerProperties.InProcess::name)
                    .orElse(null);
        }

        beanToInjected.keySet().forEach(this::injectFields);
    }

    @Override
    public void destroy() {
        beanToInjected.clear();
    }

    private boolean isTestInstance(Object bean) {
        return AnnotationUtils.findAnnotation(AopProxyUtils.ultimateTargetClass(bean), SpringBootTest.class) != null;
    }

    private boolean hasRelevantAnnotations(Object bean) {
        return Arrays.stream(AopProxyUtils.ultimateTargetClass(bean).getDeclaredFields())
                .anyMatch(field -> hasInProcessNameAnnotation(field) || hasLocalGrpcPortAnnotation(field));
    }

    private void injectFields(Object bean) {
        if (Boolean.TRUE.equals(beanToInjected.get(bean))) {
            // Already injected
            return;
        }

        ReflectionUtils.doWithFields(AopProxyUtils.ultimateTargetClass(bean), field -> {
            if (hasInProcessNameAnnotation(field)) {
                injectInProcessName(bean, field);
            } else if (hasLocalGrpcPortAnnotation(field)) {
                injectLocalGrpcPort(bean, field);
            }
        });

        beanToInjected.put(bean, true);
    }

    private static boolean hasLocalGrpcPortAnnotation(Field field) {
        return AnnotationUtils.findAnnotation(field, LocalGrpcPort.class) != null;
    }

    private static boolean hasInProcessNameAnnotation(Field field) {
        return AnnotationUtils.findAnnotation(field, InProcessName.class) != null;
    }

    private void injectInProcessName(Object bean, Field field) {
        if (inProcessName == null) {
            return;
        }
        Class<?> type = field.getType();
        if (type != String.class) {
            throw new UnsupportedOperationException(String.format(
                    "@InProcessName can only be applied to String field; found: %s", type.getSimpleName()));
        }
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, inProcessName);
    }

    private void injectLocalGrpcPort(Object bean, Field field) {
        Class<?> type = field.getType();
        ReflectionUtils.makeAccessible(field);

        if (type == int.class || type == Integer.class) {
            ReflectionUtils.setField(field, bean, port);
        } else if (type == long.class || type == Long.class) {
            ReflectionUtils.setField(field, bean, (long) port);
        } else if (type == String.class) {
            ReflectionUtils.setField(field, bean, String.valueOf(port));
        } else {
            throw new UnsupportedOperationException(String.format(
                    "@LocalGrpcPort can only be applied to fields of type int/Integer, long/Long, String; found: %s",
                    type.getSimpleName()));
        }
    }
}
