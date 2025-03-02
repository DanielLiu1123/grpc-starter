package grpcstarter.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * @author Freeman
 */
class GrpcClientsRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    @SuppressWarnings("unchecked")
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attrs = Optional.ofNullable(
                        metadata.getAnnotationAttributes(EnableGrpcClients.class.getName()))
                .orElse(Map.of());

        // Shouldn't scan basePackages when using 'clients' property
        String[] basePackages = (String[]) attrs.getOrDefault("value", new String[0]);
        Class<?>[] clientClasses = (Class<?>[]) attrs.getOrDefault("clients", new Class<?>[0]);
        Class<? extends GrpcClientBeanDefinitionHandler> beanDefinitionHandler =
                (Class<? extends GrpcClientBeanDefinitionHandler>) attrs.get("beanDefinitionHandler");

        GrpcStubBeanDefinitionRegistry.scanInfo.clients.addAll(List.of(clientClasses));
        GrpcStubBeanDefinitionRegistry.scanInfo.basePackages.addAll(List.of(basePackages));
        GrpcStubBeanDefinitionRegistry.scanInfo.beanDefinitionHandler = beanDefinitionHandler;

        if (basePackages.length == 0 && clientClasses.length == 0) {
            // @EnableGrpcClients should scan the package of the annotated class
            GrpcStubBeanDefinitionRegistry.scanInfo.basePackages.add(
                    ClassUtils.getPackageName(metadata.getClassName()));
        }
    }
}
