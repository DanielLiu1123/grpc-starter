package com.freemanan.starter.grpc.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.springframework.aop.framework.AopProxyUtils;
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
import org.springframework.util.unit.DataSize;

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
    private final Class<?> stubClass;

    GrpcClientCreator(BeanFactory beanFactory, Class<?> stubClass) {
        this.beanFactory = beanFactory;
        this.stubClass = stubClass;
    }

    /**
     * Create a gRPC stub instance.
     *
     * @param <T> stub type
     * @return gRPC stub instance
     */
    @SuppressWarnings({"unchecked"})
    public <T> T create() {
        Method stubMethod =
                ReflectionUtils.findMethod(stubClass.getEnclosingClass(), getStubMethodName(stubClass), Channel.class);
        Assert.notNull(stubMethod, "stubMethod must not be null");

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(
                        ManagedChannel.class, () -> new GrpcChannelCreator(beanFactory, stubClass).create())
                .getBeanDefinition();
        abd.setLazyInit(true);

        String channelBeanName = "grpc-channel-" + UUID.randomUUID();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(abd, channelBeanName);
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        GrpcClientProperties properties = beanFactory.getBean(GrpcClientProperties.class);
        if (SPRING_CLOUD_CONTEXT_PRESENT && properties.getRefresh().isEnabled()) {
            abd.setScope("refresh");
            holder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
        }
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

        ManagedChannel channel = beanFactory.getBean(channelBeanName, ManagedChannel.class);
        T stub = (T) ReflectionUtils.invokeMethod(stubMethod, null, channel);

        Assert.isTrue(stub != null, "stub must not be null");

        stub = setOptions(stub, properties);

        Cache.addStubClass(stubClass);
        return stub;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T setOptions(T stub, GrpcClientProperties properties) {
        GrpcClientProperties.Channel cfg =
                GrpcChannelCreator.getMatchedConfig(AopProxyUtils.ultimateTargetClass(stub), properties);

        GrpcClientOptions opt = new GrpcClientOptions();

        setOptionValues(opt, cfg);

        return (T) ((AbstractStub) stub).withOption(GrpcClientOptions.KEY, opt);
    }

    static void setOptionValues(GrpcClientOptions opt, GrpcClientProperties.Channel cfg) {
        Optional.ofNullable(cfg.getDeadline()).ifPresent(opt::setDeadline);
        Optional.ofNullable(cfg.getMaxOutboundMessageSize())
                .map(DataSize::toBytes)
                .map(Long::intValue)
                .ifPresent(opt::setMaxOutboundMessageSize);
        Optional.ofNullable(cfg.getCompression()).ifPresent(opt::setCompression);
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
