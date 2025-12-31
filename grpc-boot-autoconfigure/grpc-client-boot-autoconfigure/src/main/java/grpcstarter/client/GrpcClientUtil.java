package grpcstarter.client;

import static org.springframework.core.NativeDetector.inNativeImage;

import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
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
 * gRPC client utility class.
 *
 * @author Freeman
 */
public final class GrpcClientUtil {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientUtil.class);

    static final String CHANNEL_BEAN_NAME_PREFIX = "grpc-channel-";
    static final String IS_CREATED_BY_FRAMEWORK = "isCreatedByFramework";

    private static final boolean SPRING_CLOUD_CONTEXT_PRESENT =
            ClassUtils.isPresent("org.springframework.cloud.context.scope.refresh.RefreshScope", null);
    private static final boolean SUPPORT_REFRESH = !isAotProcessing() && !inNativeImage();

    private GrpcClientUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Register a gRPC client bean for the given class.
     *
     * @param beanFactory {@link DefaultListableBeanFactory}
     * @param clz         gRPC client class
     */
    public static void registerGrpcClientBean(DefaultListableBeanFactory beanFactory, Class<?> clz) {
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
        abd.setAttribute(IS_CREATED_BY_FRAMEWORK, true);
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
        checkValid(channelConfig);

        String channelBeanName = CHANNEL_BEAN_NAME_PREFIX + channelConfig.getName();
        if (beanFactory.containsBean(channelBeanName)) {
            return;
        }

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(
                        ManagedChannel.class, () -> createChannel(beanFactory, channelConfig))
                .getBeanDefinition();

        abd.setAttribute(IS_CREATED_BY_FRAMEWORK, true);
        abd.setResourceDescription("Auto registered by grpc-client-boot-starter");

        BeanDefinitionHolder holder = new BeanDefinitionHolder(abd, channelBeanName);
        if (SUPPORT_REFRESH
                && SPRING_CLOUD_CONTEXT_PRESENT
                && getRefresh(environment).isEnabled()) {
            abd.setScope("refresh");
            holder = ScopedProxyUtils.createScopedProxy(holder, beanFactory, true);
        }

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, beanFactory);
    }

    private static boolean isValid(GrpcClientProperties.Channel channelConfig) {
        return StringUtils.hasText(channelConfig.getAuthority())
                || (channelConfig.getInProcess() != null
                        && StringUtils.hasText(channelConfig.getInProcess().name()));
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

        // Default channel may not have authority or in-process name
        var defaultChannel = properties.defaultChannel();
        if (isValid(defaultChannel)) {
            registerGrpcChannelBean(beanFactory, environment, defaultChannel);
        }
    }

    static GrpcClientProperties.Refresh getRefresh(Environment environment) {
        return Binder.get(environment)
                .bind(GrpcClientProperties.Refresh.PREFIX, GrpcClientProperties.Refresh.class)
                .orElseGet(GrpcClientProperties.Refresh::new);
    }

    static ManagedChannel createChannel(BeanFactory beanFactory, GrpcClientProperties.Channel channelConfig) {
        checkValid(channelConfig);
        return new GrpcChannelCreator(beanFactory, channelConfig).create();
    }

    private static void checkValid(GrpcClientProperties.Channel channelConfig) {
        if (!isValid(channelConfig)) {
            throw new IllegalStateException(
                    "Channel authority or in-process name must be configured, name: " + channelConfig.getName());
        }
    }

    /**
     * @see AbstractAotProcessor#process()
     */
    private static boolean isAotProcessing() {
        return Boolean.getBoolean("spring.aot.processing");
    }
}
