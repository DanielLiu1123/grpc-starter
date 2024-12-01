package grpcstarter.client;

import io.grpc.stub.AbstractStub;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;

/**
 * @author Freeman
 */
class GrpcStubBeanRegistrar {
    private static final Logger log = LoggerFactory.getLogger(GrpcStubBeanRegistrar.class);

    private final ClassPathScanningCandidateComponentProvider scanner = getScanner();
    private final BeanDefinitionRegistry registry;

    private Map<Class<?>, List<BeanDefinition>> classToBeanDefinitions;

    public GrpcStubBeanRegistrar(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Register gRPC stub beans for base packages.
     *
     * @param basePackages base packages to scan
     */
    public void register(String... basePackages) {
        for (String pkg : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(pkg);
            for (BeanDefinition bd : beanDefinitions) {
                var clz = Util.getBeanDefinitionClass(bd);
                if (clz != null) {
                    registerGrpcStubBean(clz);
                }
            }
        }
    }

    public void register(Class<?>... clients) {
        for (var client : clients) {
            registerGrpcStubBean(client);
        }
    }

    private void registerGrpcStubBean(Class<?> clz) {
        if (!(registry instanceof DefaultListableBeanFactory dlb)) {
            throw new IllegalArgumentException("registry must be instance of DefaultListableBeanFactory");
        }

        initClassToBeanDefinitions(dlb);

        if (classToBeanDefinitions.containsKey(clz)) {
            if (log.isDebugEnabled()) {
                log.debug("gRPC client bean '{}' is already registered, skip auto registration", clz.getName());
            }
            return;
        }

        GrpcClientUtil.registerGrpcClientBean(dlb, clz);
    }

    private void initClassToBeanDefinitions(DefaultListableBeanFactory bf) {
        if (classToBeanDefinitions == null) {
            classToBeanDefinitions = new HashMap<>();
            for (var beanDefinitionName : bf.getBeanDefinitionNames()) {
                var beanDefinition = bf.getBeanDefinition(beanDefinitionName);
                var clz = Util.getBeanDefinitionClass(beanDefinition);
                if (clz != null) {
                    classToBeanDefinitions
                            .computeIfAbsent(clz, k -> new ArrayList<>())
                            .add(beanDefinition);
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
