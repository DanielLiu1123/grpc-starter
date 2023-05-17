package com.freemanan.starter.grpc.client;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
class GrpcClientsRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;
    private GrpcClientProperties properties;
    private GrpcStubBeanRegistrar registrar;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        boolean enabled = environment.getProperty(GrpcClientProperties.PREFIX + ".enabled", Boolean.class, true);
        if (!enabled) {
            return;
        }
        init(registry);

        Map<String, Object> attrs = Optional.ofNullable(
                        metadata.getAnnotationAttributes(EnableGrpcClients.class.getName()))
                .orElse(Collections.emptyMap());

        // Shouldn't scan base packages when using clients property only
        Class<?>[] clientClasses = (Class<?>[]) attrs.getOrDefault("clients", new Class<?>[0]);
        String[] basePackages = getBasePackages(attrs);
        if (clientClasses.length > 0) {
            registerBeans4Classes(clientClasses);
            if (basePackages.length > 0) {
                // @EnableGrpcClients(basePackages = "com.example", clients = {xxStub.class})
                // should scan basePackages and register specified clients
                registrar.register(basePackages);
            }
            return;
        }

        if (basePackages.length == 0) {
            // @EnableGrpcClients should scan the package of the annotated class
            basePackages = new String[] {ClassUtils.getPackageName(metadata.getClassName())};
        }

        registrar.register(basePackages);
    }

    private void init(BeanDefinitionRegistry registry) {
        this.properties = (properties == null ? Util.getProperties(environment) : properties);
        this.registrar = (registrar == null ? new GrpcStubBeanRegistrar(properties, registry) : registrar);
    }

    private String[] getBasePackages(Map<String, Object> attrs) {
        // Configuration takes effect only when @EnableGrpcClients not set basePackages
        String[] basePackages = (String[]) attrs.getOrDefault("value", new String[0]);
        if (ObjectUtils.isEmpty(basePackages)) {
            return properties.getBasePackages().toArray(new String[0]);
        }
        return basePackages;
    }

    private void registerBeans4Classes(Class<?>[] classes) {
        for (Class<?> clz : classes) {
            registrar.registerGrpcStubBean(clz.getName());
        }
    }
}
