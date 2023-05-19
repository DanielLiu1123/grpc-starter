package com.freemanan.starter.grpc.extensions.test;

import com.freemanan.starter.grpc.server.GrpcServerStartedEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
public class GrpcPortListener implements ApplicationListener<GrpcServerStartedEvent>, BeanPostProcessor {

    private final List<Object> beans = new ArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
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
        injectPort(event);
    }

    private void injectPort(GrpcServerStartedEvent event) {
        int port = event.getSource().getPort();
        for (Object bean : beans) {
            ReflectionUtils.doWithFields(AopProxyUtils.ultimateTargetClass(bean), field -> {
                if (AnnotationUtils.findAnnotation(field, LocalGrpcPort.class) != null) {
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, bean, port);
                }
            });
        }
    }
}
