package com.freemanan.starter.grpc.extensions.test;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import com.freemanan.starter.grpc.server.GrpcServerStartedEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
class GrpcTestBeanPostProcessor
        implements ApplicationListener<GrpcServerStartedEvent>, BeanPostProcessor, BeanFactoryAware {

    private final Map<Object, Boolean> beansToInject = new HashMap<>();

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
            beansToInject.put(bean, false);
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
        this.inProcessName = Optional.ofNullable(
                        beanFactory.getBean(GrpcServerProperties.class).getInProcess())
                .map(GrpcServerProperties.InProcess::getName)
                .orElse(null);

        beansToInject.keySet().forEach(this::injectFields);
    }

    private boolean isTestInstance(Object bean) {
        return AnnotationUtils.findAnnotation(AopProxyUtils.ultimateTargetClass(bean), SpringBootTest.class) != null;
    }

    private boolean hasRelevantAnnotations(Object bean) {
        return Arrays.stream(AopProxyUtils.ultimateTargetClass(bean).getDeclaredFields())
                .anyMatch(field -> AnnotationUtils.findAnnotation(field, InProcessName.class) != null
                        || AnnotationUtils.findAnnotation(field, LocalGrpcPort.class) != null);
    }

    private void injectFields(Object bean) {
        if (Boolean.TRUE.equals(beansToInject.get(bean))) {
            // Already injected
            return;
        }
        ReflectionUtils.doWithFields(AopProxyUtils.ultimateTargetClass(bean), field -> {
            if (AnnotationUtils.findAnnotation(field, InProcessName.class) != null) {
                if (inProcessName == null) {
                    return;
                }
                ReflectionUtils.makeAccessible(field);
                Class<?> type = field.getType();
                if (type != String.class) {
                    throw new UnsupportedOperationException(String.format(
                            "@InProcessName can only be applied to String field; found: %s", type.getSimpleName()));
                }
                ReflectionUtils.setField(field, bean, inProcessName);
            } else if (AnnotationUtils.findAnnotation(field, LocalGrpcPort.class) != null) {
                ReflectionUtils.makeAccessible(field);
                Class<?> type = field.getType();
                if (type == int.class || type == Integer.class) {
                    ReflectionUtils.setField(field, bean, port);
                } else if (type == long.class || type == Long.class) {
                    ReflectionUtils.setField(field, bean, (long) port);
                } else if (type == String.class) {
                    ReflectionUtils.setField(field, bean, String.valueOf(port));
                } else {
                    throw new UnsupportedOperationException(String.format(
                            "@LocalGrpcPort can only be applied to fields of type int/Integer, long/Long, String; "
                                    + "found: %s",
                            type.getSimpleName()));
                }
            }
        });
        beansToInject.put(bean, true);
    }
}
