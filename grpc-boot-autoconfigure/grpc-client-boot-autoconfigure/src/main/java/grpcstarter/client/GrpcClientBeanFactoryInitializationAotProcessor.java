package grpcstarter.client;

import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.env.Environment;
import org.springframework.javapoet.MethodSpec;

/**
 * TIP: use 'processAot' task to debug the AOT processing.
 *
 * @author Freeman
 */
class GrpcClientBeanFactoryInitializationAotProcessor
        implements BeanRegistrationExcludeFilter, BeanFactoryInitializationAotProcessor {

    static final String IS_CREATED_BY_FRAMEWORK = "isCreatedByFramework";

    @Override
    public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
        // Separate manually registered beans from those registered by Spring
        var isGrpcClient = AbstractStub.class.isAssignableFrom(registeredBean.getBeanClass());
        var isCreatedByFramework = registeredBean.getMergedBeanDefinition().hasAttribute(IS_CREATED_BY_FRAMEWORK);
        return isGrpcClient && isCreatedByFramework;
    }

    @Nullable
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            @Nonnull ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            var beanNameToBeanDefinition = listDefinition(beanFactory);
            if (beanNameToBeanDefinition.isEmpty()) {
                return;
            }

            // Add a method to register gRPC client beans
            var methodReference = beanFactoryInitializationCode
                    .getMethods()
                    .add("registerGrpcClientBeanDefinitions", method -> buildMethod(method, beanNameToBeanDefinition))
                    .toMethodReference();
            beanFactoryInitializationCode.addInitializer(methodReference);

            var reflection = generationContext.getRuntimeHints().reflection();

            // Register reflection metadata for gRPC client beans
            for (var entry : beanNameToBeanDefinition.entrySet()) {
                var beanDefinition = entry.getValue();
                var clientClass = beanDefinition.getResolvableType().resolve();
                if (clientClass == null) {
                    continue;
                }

                var grpcClass = clientClass.getEnclosingClass();
                if (grpcClass == null) {
                    continue;
                }

                reflection.registerType(grpcClass, builder -> {

                    // io.grpc.testing.protobuf.SimpleServiceGrpc.newBlockingStub(Channel)
                    builder.withMethod(
                            GrpcClientCreator.NEW_BLOCKING_STUB_METHOD,
                            List.of(TypeReference.of(Channel.class)),
                            ExecutableMode.INVOKE);
                    builder.withMethod(
                            GrpcClientCreator.NEW_STUB_METHOD,
                            List.of(TypeReference.of(Channel.class)),
                            ExecutableMode.INVOKE);
                    builder.withMethod(
                            GrpcClientCreator.NEW_FUTURE_STUB_METHOD,
                            List.of(TypeReference.of(Channel.class)),
                            ExecutableMode.INVOKE);

                    // io.grpc.testing.protobuf.SimpleServiceGrpc#SERVICE_NAME
                    builder.withField(Util.SERVICE_NAME);
                });
            }
        };
    }

    private static void buildMethod(MethodSpec.Builder method, Map<String, BeanDefinition> definitions) {
        method.addModifiers(Modifier.PUBLIC);
        // See org.springframework.beans.factory.aot.BeanFactoryInitializationCode.addInitializer
        // Support DefaultListableBeanFactory, Environment, and ResourceLoader
        method.addParameter(DefaultListableBeanFactory.class, "beanFactory");
        method.addParameter(Environment.class, "environment");
        definitions.forEach((beanName, beanDefinition) -> {
            Class<?> clientClass = beanDefinition.getResolvableType().resolve();
            method.addStatement(
                    "$T.registerGrpcClientBean(beanFactory, environment, $T.class)", GrpcClientUtil.class, clientClass);
        });
    }

    private static Map<String, BeanDefinition> listDefinition(ConfigurableListableBeanFactory beanFactory) {
        var beanDefinitions = new HashMap<String, BeanDefinition>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
            Class<?> clz = beanDefinition.getResolvableType().resolve();
            if (clz != null && AbstractStub.class.isAssignableFrom(clz)) {
                beanDefinitions.put(name, beanDefinition);
            }
        }
        return beanDefinitions;
    }
}
