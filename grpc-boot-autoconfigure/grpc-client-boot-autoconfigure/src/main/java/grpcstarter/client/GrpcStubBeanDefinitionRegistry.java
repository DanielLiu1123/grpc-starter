package grpcstarter.client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

/**
 * {@link GrpcStubBeanDefinitionRegistry} registers the bean definitions for gRPC stubs based on the configuration provided.
 *
 * @author Freeman
 */
class GrpcStubBeanDefinitionRegistry implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    static final ScanInfo scanInfo = new ScanInfo();

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
        GrpcClientCreator.setChannelName(properties);

        registerChannels(registry, properties);

        registerStubs(new GrpcStubBeanRegistrar(registry), properties);
    }

    private void registerChannels(BeanDefinitionRegistry registry, GrpcClientProperties properties) {
        for (GrpcClientProperties.Channel channelConfig : properties.getChannels()) {
            String channelName = channelConfig.getName();
        }
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

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }
}
