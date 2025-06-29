package grpcstarter.client;

import static org.springframework.core.NativeDetector.inNativeImage;

import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
public final class GrpcClientUtil {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientUtil.class);

    private static final boolean springCloudContextPresent =
            ClassUtils.isPresent("org.springframework.cloud.context.scope.refresh.RefreshScope", null);
    private static final String channelBeanNamePrefix = "grpc-channel-";
    private static final boolean supportRefresh = !isAotProcessing() && !inNativeImage();

    private GrpcClientUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Register a gRPC client bean for the given class.
     *
     * @param beanFactory {@link DefaultListableBeanFactory}
     * @param clz         gRPC client class
     */
    public static void registerGrpcClientBean(@Nonnull DefaultListableBeanFactory beanFactory, @Nonnull Class<?> clz) {
        if (!AbstractStub.class.isAssignableFrom(clz)) {
            throw new IllegalArgumentException(clz + " is not a gRPC client");
        }

        var className = clz.getName();

        // Refresh scope is not supported with native images, see
        // https://docs.spring.io/spring-cloud-config/reference/server/aot-and-native-image-support.html

        var abd = BeanDefinitionBuilder.genericBeanDefinition(
                        clz, () -> new GrpcClientCreator(beanFactory, clz).create())
                .getBeanDefinition();

        abd.setLazyInit(true);
        abd.setAttribute(GrpcClientBeanFactoryInitializationAotProcessor.IS_CREATED_BY_FRAMEWORK, true);
        abd.setResourceDescription("Auto registered by grpc-client-boot-starter");

        // TODO(Freeman): beanDefinitionHandler not working in AOT
        BeanDefinition definitionToUse = abd;
        if (GrpcStubBeanDefinitionRegistry.scanInfo.beanDefinitionHandler != null) {
            GrpcClientBeanDefinitionHandler beanDefinitionHandler =
                    BeanUtils.instantiateClass(GrpcStubBeanDefinitionRegistry.scanInfo.beanDefinitionHandler);
            definitionToUse = beanDefinitionHandler.handle(abd, clz);
        }

        if (definitionToUse == null) {
            return;
        }

        try {
            BeanDefinitionReaderUtils.registerBeanDefinition(
                    new BeanDefinitionHolder(definitionToUse, className), beanFactory);
        } catch (BeanDefinitionOverrideException ignore) {
            // clients are included in base packages
            log.warn(
                    "gRPC stub '{}' is included in base packages, you can remove it from 'clients' property.",
                    className);
        }
    }

    /**
     * Register a gRPC channel bean for the given channel configuration.
     *
     * <p> This method is used in AOT processing.
     *
     * @param beanFactory   {@link DefaultListableBeanFactory}
     * @param channelConfig gRPC channel configuration
     */
    public static void registerGrpcChannelBean(
            DefaultListableBeanFactory beanFactory,
            Environment environment,
            GrpcClientProperties.Channel channelConfig) {
        if (!StringUtils.hasText(channelConfig.getAuthority())) {
            throw new IllegalStateException("Channel authority must not be empty, name: " + channelConfig.getName());
        }

        String channelBeanName = channelBeanNamePrefix + channelConfig.getName();
        if (beanFactory.containsBean(channelBeanName)) {
            return;
        }

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(
                        ManagedChannel.class, () -> createChannel(beanFactory, channelConfig))
                .getBeanDefinition();

        abd.setAttribute(GrpcClientBeanFactoryInitializationAotProcessor.IS_CREATED_BY_FRAMEWORK, true);
        abd.setResourceDescription("Auto registered by grpc-client-boot-starter");

        BeanDefinitionHolder holder = new BeanDefinitionHolder(abd, channelBeanName);
        if (supportRefresh
                && springCloudContextPresent
                && getRefresh(environment).isEnabled()) {
            abd.setScope("refresh");
            holder = ScopedProxyUtils.createScopedProxy(holder, beanFactory, true);
        }

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, beanFactory);
    }

    /**
     * Register gRPC channel beans for the given environment.
     *
     * <p> This method is used in AOT processing.
     *
     * @param beanFactory bean factory
     * @param environment environment
     */
    public static void registerGrpcChannelBeans(DefaultListableBeanFactory beanFactory, Environment environment) {
        var properties = Util.getProperties(environment);
        registerGrpcChannelBeans(beanFactory, environment, properties);
    }

    /**
     * Internal usage.
     */
    static void registerGrpcChannelBeans(
            DefaultListableBeanFactory beanFactory, Environment environment, GrpcClientProperties properties) {
        for (var channelConfig : properties.getChannels()) {
            registerGrpcChannelBean(beanFactory, environment, channelConfig);
        }

        var defaultChannel = properties.defaultChannel();
        if (StringUtils.hasText(defaultChannel.getAuthority())) {
            registerGrpcChannelBean(beanFactory, environment, defaultChannel);
        }
    }

    private static GrpcClientProperties.Refresh getRefresh(Environment environment) {
        return Binder.get(environment)
                .bind(GrpcClientProperties.Refresh.PREFIX, GrpcClientProperties.Refresh.class)
                .orElseGet(GrpcClientProperties.Refresh::new);
    }

    private static ManagedChannel createChannel(BeanFactory beanFactory, GrpcClientProperties.Channel channelConfig) {
        if (!StringUtils.hasText(channelConfig.getAuthority())) {
            throw new IllegalStateException("Channel authority must not be empty, name: " + channelConfig.getName());
        }
        return new GrpcChannelCreator(beanFactory, channelConfig).create();
    }

    /**
     * @see AbstractAotProcessor#process()
     */
    private static boolean isAotProcessing() {
        return Boolean.getBoolean("spring.aot.processing");
    }
}
