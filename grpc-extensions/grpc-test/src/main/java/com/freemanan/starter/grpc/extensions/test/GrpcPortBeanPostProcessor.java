package com.freemanan.starter.grpc.extensions.test;

import com.freemanan.starter.grpc.server.GrpcServerStartedEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
class GrpcPortBeanPostProcessor implements ApplicationListener<GrpcServerStartedEvent>, BeanPostProcessor {

    private final List<Object> beans = new ArrayList<>();
    private int port;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        // test class instance logic
        if (AnnotationUtils.findAnnotation(AopProxyUtils.ultimateTargetClass(bean), SpringBootTest.class) != null) {
            // This bean is test class instance, test class instance inject fields after Spring context initialization
            // see org.springframework.test.context.support.DependencyInjectionTestExecutionListener#injectDependencies
            beans.add(bean);
            injectPort();
            return bean;
        }

        // normal bean logic
        Class<?> searchType = AopProxyUtils.ultimateTargetClass(bean);
        while (Object.class != searchType && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                if (AnnotationUtils.findAnnotation(field, LocalGrpcPort.class) != null) {
                    beans.add(bean);
                    return bean;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(GrpcServerStartedEvent event) {
        this.port = event.getSource().getPort();
    }

    private void injectPort() {
        for (Object bean : beans) {
            ReflectionUtils.doWithFields(AopProxyUtils.ultimateTargetClass(bean), field -> {
                if (AnnotationUtils.findAnnotation(field, LocalGrpcPort.class) != null) {
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
        }
    }
}
