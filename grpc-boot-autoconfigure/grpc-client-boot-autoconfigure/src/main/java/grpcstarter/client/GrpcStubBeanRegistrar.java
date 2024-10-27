package grpcstarter.client;

import io.grpc.stub.AbstractStub;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
class GrpcStubBeanRegistrar {

    private static final Set<BeanDefinitionRegistry> registries = ConcurrentHashMap.newKeySet();

    private final ClassPathScanningCandidateComponentProvider scanner;
    private final GrpcClientProperties properties;
    private final BeanDefinitionRegistry registry;

    public GrpcStubBeanRegistrar(GrpcClientProperties properties, BeanDefinitionRegistry registry) {
        this(properties, registry, getScanner());
    }

    public GrpcStubBeanRegistrar(
            GrpcClientProperties properties,
            BeanDefinitionRegistry registry,
            ClassPathScanningCandidateComponentProvider scanner) {
        this.properties = properties;
        this.registry = registry;
        this.scanner = scanner;
        registries.add(registry);
    }

    /**
     * Register gRPC stub beans for base packages, using {@link GrpcClientProperties#getBasePackages()} if not specified.
     *
     * @param basePackages base packages to scan
     */
    public void register(String... basePackages) {
        List<String> packages =
                ObjectUtils.isEmpty(basePackages) ? properties.getBasePackages() : Arrays.asList(basePackages);
        registerBeans4BasePackages(packages);
    }

    @SneakyThrows
    public void registerGrpcStubBean(String className) {
        Class<?> clz = Class.forName(className);

        Assert.isInstanceOf(BeanFactory.class, registry, "BeanDefinitionRegistry must be instance of BeanFactory");

        GrpcClientUtil.registerGrpcClientBean((DefaultListableBeanFactory) registry, clz);
    }

    /**
     * @return whether this {@link BeanDefinitionRegistry} has been registered
     */
    public static boolean hasRegistered(BeanDefinitionRegistry registry) {
        return registries.contains(registry);
    }

    private void registerBeans4BasePackages(List<String> basePackages) {
        for (String pkg : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(pkg);
            for (BeanDefinition bd : beanDefinitions) {
                if (bd.getBeanClassName() != null) {
                    registerGrpcStubBean(bd.getBeanClassName());
                }
            }
        }
    }

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider s = new ClassPathScanningCandidateComponentProvider(false);
        s.addIncludeFilter((metadataReader, metadataReaderFactory) -> isGrpcStub(metadataReader));
        return s;
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
            // ignore
        }
        return false;
    }
}
