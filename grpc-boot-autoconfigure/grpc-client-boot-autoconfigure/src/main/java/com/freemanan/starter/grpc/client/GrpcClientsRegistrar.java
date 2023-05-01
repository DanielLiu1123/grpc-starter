package com.freemanan.starter.grpc.client;

import io.grpc.stub.AbstractStub;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Freeman
 */
class GrpcClientsRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private static final Logger log = LoggerFactory.getLogger(GrpcClientsRegistrar.class);

    private ResourceLoader resourceLoader;

    private Environment environment;

    private GrpcClientProperties properties;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        this.properties = getProperties(environment);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        if (!properties.isEnabled()) {
            return;
        }

        ClassPathScanningCandidateComponentProvider scanner = getScanner();

        Map<String, Object> attrs = Optional.ofNullable(
                        metadata.getAnnotationAttributes(EnableGrpcClients.class.getName()))
                .orElse(Collections.emptyMap());

        // Shouldn't scan base packages when using clients property only
        Class<?>[] clientClasses = (Class<?>[]) attrs.getOrDefault("clients", new Class<?>[0]);
        String[] basePackages = (String[]) attrs.getOrDefault("value", new String[0]);
        if (clientClasses.length > 0) {
            registerBeans4Classes(registry, clientClasses);
            if (basePackages.length > 0) {
                // @EnableGrpcClients(basePackages = "com.example", clients = {xxStub.class})
                // should scan basePackages and register specified clients
                registerBeans4BasePackages(registry, scanner, basePackages);
            }
            return;
        }

        if (basePackages.length == 0) {
            // @EnableGrpcClients should scan the package of the annotated class
            basePackages = new String[] {ClassUtils.getPackageName(metadata.getClassName())};
        }

        registerBeans4BasePackages(registry, scanner, basePackages);
    }

    private static GrpcClientProperties getProperties(Environment environment) {
        GrpcClientProperties properties = Binder.get(environment)
                .bind(GrpcClientProperties.PREFIX, GrpcClientProperties.class)
                .orElseGet(GrpcClientProperties::new);
        properties.merge();
        return properties;
    }

    private void registerBeans4BasePackages(
            BeanDefinitionRegistry registry,
            ClassPathScanningCandidateComponentProvider scanner,
            String[] basePackages) {
        for (String pkg : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(pkg);
            for (BeanDefinition bd : beanDefinitions) {
                if (bd.getBeanClassName() != null) {
                    registerGrpcStubBean(registry, bd.getBeanClassName());
                }
            }
        }
    }

    private void registerBeans4Classes(BeanDefinitionRegistry registry, Class<?>[] classes) {
        for (Class<?> clz : classes) {
            registerGrpcStubBean(registry, clz.getName());
        }
    }

    private void registerGrpcStubBean(BeanDefinitionRegistry registry, String className) {
        Class<?> clz;
        try {
            clz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Assert.isInstanceOf(
                ConfigurableBeanFactory.class,
                registry,
                "BeanDefinitionRegistry must be instance of ConfigurableBeanFactory");

        GrpcClientCreator creator = new GrpcClientCreator((ConfigurableBeanFactory) registry, properties, clz);

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(clz, creator::create)
                .getBeanDefinition();

        abd.setLazyInit(true);

        try {
            registry.registerBeanDefinition(className, abd);
        } catch (BeanDefinitionOverrideException ignore) {
            // clients are included in base packages
            log.warn(
                    "Your gRPC stub '{}' is included in base packages, you can remove it from 'clients' property.",
                    className);
        }
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, this.environment);
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> isGrpcStub(metadataReader));
        return scanner;
    }

    private static boolean isGrpcStub(MetadataReader mr) {
        ClassMetadata cm = mr.getClassMetadata();
        boolean isStub = cm.hasSuperClass()
                && cm.hasEnclosingClass()
                && cm.getClassName().endsWith("Stub");
        if (!isStub) {
            return false;
        }
        try {
            Class<?> clz = Class.forName(cm.getClassName());
            if (AbstractStub.class.isAssignableFrom(clz)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }
}
