package grpcstarter.client;

import static org.springframework.core.NativeDetector.inNativeImage;

import io.grpc.ManagedChannel;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link GrpcStubBeanDefinitionRegistry} registers the bean definitions for gRPC stubs based on the configuration provided.
 *
 * @author Freeman
 */
class GrpcStubBeanDefinitionRegistry implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    static final ScanInfo scanInfo = new ScanInfo();
    static final String channelBeanNamePrefix = "grpc-channel-";
    static final boolean supportRefresh = !isAotProcessing() && !inNativeImage();

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        boolean enabled = environment.getProperty(GrpcClientProperties.PREFIX + ".enabled", Boolean.class, true);
        if (!enabled) {
            return;
        }

        var properties = Util.getProperties(environment);

        registerChannels(registry, properties);

        registerStubs(new GrpcStubBeanRegistrar(registry), properties);
    }

    private void registerChannels(BeanDefinitionRegistry registry, GrpcClientProperties properties) {
        for (var channelConfig : properties.getChannels()) {
            registerChannel(registry, properties, channelConfig);
        }

        var defaultChannel = properties.defaultChannel();
        if (StringUtils.hasText(defaultChannel.getAuthority())) {
            registerChannel(registry, properties, defaultChannel);
        }
    }

    private void registerChannel(
            BeanDefinitionRegistry registry,
            GrpcClientProperties properties,
            GrpcClientProperties.Channel channelConfig) {
        var bf = (BeanFactory) registry;

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(
                        ManagedChannel.class, () -> createChannel(bf, channelConfig))
                .getBeanDefinition();

        String channelBeanName = channelBeanNamePrefix + channelConfig.getName();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(abd, channelBeanName);
        if (supportRefresh
                && GrpcClientCreator.SPRING_CLOUD_CONTEXT_PRESENT
                && properties.getRefresh().isEnabled()) {
            abd.setScope("refresh");
            holder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
        }
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private void registerStubs(GrpcStubBeanRegistrar registrar, GrpcClientProperties properties) {

        // NOTE: @EnableGrpcClients has higher priority than properties
        // we need to check if @EnableGrpcClients set the beanDefinitionHandler first
        if ((scanInfo.beanDefinitionHandler == null // not use @EnableGrpcClients
                        || scanInfo.beanDefinitionHandler
                                == GrpcClientBeanDefinitionHandler.Default.class) // not set beanDefinitionHandler
                && properties.getBeanDefinitionHandler() != null) {
            scanInfo.beanDefinitionHandler = properties.getBeanDefinitionHandler();
        }

        scanInfo.basePackages.addAll(properties.getBasePackages());
        scanInfo.clients.addAll(properties.getClients());

        if (!ObjectUtils.isEmpty(scanInfo.basePackages)) {
            registrar.register(scanInfo.basePackages.toArray(String[]::new));
        }

        if (!ObjectUtils.isEmpty(scanInfo.clients)) {
            registrar.register(scanInfo.clients.toArray(Class<?>[]::new));
        }
    }

    private ManagedChannel createChannel(BeanFactory beanFactory, GrpcClientProperties.Channel channelConfig) {
        if (!StringUtils.hasText(channelConfig.getAuthority())) {
            throw new IllegalStateException("Channel authority must not be empty, name: " + channelConfig.getName());
        }
        return new GrpcChannelCreator(beanFactory, channelConfig).create();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

    /**
     * @see AbstractAotProcessor#process()
     */
    private static boolean isAotProcessing() {
        return Boolean.getBoolean("spring.aot.processing");
    }
}
