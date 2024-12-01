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
        registerBeans(new GrpcStubBeanRegistrar(registry));
    }

    private void registerBeans(GrpcStubBeanRegistrar registrar) {
        var properties = Util.getProperties(environment);
        scanInfo.basePackages.addAll(properties.getBasePackages());
        if (!ObjectUtils.isEmpty(scanInfo.basePackages)) {
            registrar.register(scanInfo.basePackages.toArray(String[]::new));
        }
        scanInfo.clients.addAll(properties.getClients());
        if (!ObjectUtils.isEmpty(scanInfo.clients)) {
            registrar.register(scanInfo.clients.toArray(Class<?>[]::new));
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }
}
