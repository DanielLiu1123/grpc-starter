package com.freemanan.starter.grpc.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.lang.reflect.Method;
import java.util.UUID;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
class GrpcClientCreator {
    private static final String NEW_BLOCKING_STUB_METHOD = "newBlockingStub";
    private static final String NEW_FUTURE_STUB_METHOD = "newFutureStub";
    private static final String NEW_STUB_METHOD = "newStub";
    private static final String BLOCKING_STUB = "BlockingStub";
    private static final String FUTURE_STUB = "FutureStub";

    private static final boolean SPRING_CLOUD_CONTEXT_PRESENT =
            ClassUtils.isPresent("org.springframework.cloud.context.scope.refresh.RefreshScope", null);

    private final BeanFactory beanFactory;
    private final GrpcClientProperties properties;
    private final Class<?> stubClass;

    GrpcClientCreator(BeanFactory beanFactory, GrpcClientProperties properties, Class<?> stubClass) {
        this.beanFactory = beanFactory;
        this.properties = properties;
        this.stubClass = stubClass;
    }

    /**
     * Create a gRPC stub instance.
     *
     * @param <T> stub type
     * @return gRPC stub instance
     */
    @SuppressWarnings("unchecked")
    public <T> T create() {
        Method stubMethod =
                ReflectionUtils.findMethod(stubClass.getEnclosingClass(), getStubMethodName(stubClass), Channel.class);
        Assert.notNull(stubMethod, "stubMethod must not be null");

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(
                        ManagedChannel.class, () -> new GrpcChannelCreator(beanFactory, stubClass, properties).create())
                .getBeanDefinition();
        abd.setLazyInit(true);

        String channelBeanName = UUID.randomUUID().toString();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(abd, channelBeanName);
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        if (SPRING_CLOUD_CONTEXT_PRESENT && properties.getRefresh().isEnabled()) {
            abd.setScope("refresh");
            holder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
        }
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

        ManagedChannel channel = beanFactory.getBean(channelBeanName, ManagedChannel.class);
        T stub = (T) ReflectionUtils.invokeMethod(stubMethod, null, channel);
        Cache.addStubClass(stubClass);
        return stub;
    }

    private static String getStubMethodName(Class<?> stubClass) {
        if (stubClass.getName().endsWith(BLOCKING_STUB)) {
            return NEW_BLOCKING_STUB_METHOD;
        } else if (stubClass.getName().endsWith(FUTURE_STUB)) {
            return NEW_FUTURE_STUB_METHOD;
        } else {
            return NEW_STUB_METHOD;
        }
    }
}
