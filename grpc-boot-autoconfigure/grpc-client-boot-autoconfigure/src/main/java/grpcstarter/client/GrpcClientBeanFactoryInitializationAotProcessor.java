package grpcstarter.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.jspecify.annotations.Nullable;
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

    @Override
    public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
        // Separate manually registered beans from those registered by Spring
        return isGrpcClient(registeredBean) || isManagedChannel(registeredBean);
    }

    @Nullable
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            var clientBeanDefinitions = listClientDefinitions(beanFactory);
            var channelBeanDefinitions = listChannelDefinitions(beanFactory);

            if (!channelBeanDefinitions.isEmpty()) {
                // Add a method to register gRPC channel beans
                var channelMethodReference = beanFactoryInitializationCode
                        .getMethods()
                        .add(
                                "registerGrpcChannelBeanDefinitions",
                                GrpcClientBeanFactoryInitializationAotProcessor::buildChannelMethod)
                        .toMethodReference();
                beanFactoryInitializationCode.addInitializer(channelMethodReference);
            }

            if (!clientBeanDefinitions.isEmpty()) {
                // Add a method to register gRPC client beans
                var clientMethodReference = beanFactoryInitializationCode
                        .getMethods()
                        .add(
                                "registerGrpcClientBeanDefinitions",
                                method -> buildClientMethod(method, clientBeanDefinitions))
                        .toMethodReference();
                beanFactoryInitializationCode.addInitializer(clientMethodReference);
            }

            var reflection = generationContext.getRuntimeHints().reflection();

            // Register reflection metadata for gRPC client beans
            for (var entry : clientBeanDefinitions.entrySet()) {
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

                    // From gRPC 1.70.0, newBlockingV2Stub is added
                    if (Arrays.stream(grpcClass.getMethods())
                            .anyMatch(e -> e.getName().equals(GrpcClientCreator.NEW_BLOCKING_V2_STUB_METHOD))) {
                        builder.withMethod(
                                GrpcClientCreator.NEW_BLOCKING_V2_STUB_METHOD,
                                List.of(TypeReference.of(Channel.class)),
                                ExecutableMode.INVOKE);
                    }

                    // io.grpc.testing.protobuf.SimpleServiceGrpc#SERVICE_NAME
                    builder.withField(Util.SERVICE_NAME);
                });
            }
        };
    }

    private static void buildClientMethod(MethodSpec.Builder method, Map<String, BeanDefinition> definitions) {
        method.addModifiers(Modifier.PUBLIC);
        // See org.springframework.beans.factory.aot.BeanFactoryInitializationCode.addInitializer
        // Support DefaultListableBeanFactory, Environment, and ResourceLoader
        method.addParameter(DefaultListableBeanFactory.class, "beanFactory");
        definitions.forEach((beanName, beanDefinition) -> {
            Class<?> clientClass = beanDefinition.getResolvableType().resolve();
            method.addStatement("$T.registerGrpcClientBean(beanFactory, $T.class)", GrpcClientUtil.class, clientClass);
        });
    }

    private static void buildChannelMethod(MethodSpec.Builder method) {
        method.addModifiers(Modifier.PUBLIC);
        // See org.springframework.beans.factory.aot.BeanFactoryInitializationCode.addInitializer
        // Support DefaultListableBeanFactory, Environment, and ResourceLoader
        method.addParameter(DefaultListableBeanFactory.class, "beanFactory");
        method.addParameter(Environment.class, "environment");

        // Get GrpcClientProperties from bean factory
        method.addStatement("$T.registerGrpcChannelBeans(beanFactory, environment)", GrpcClientUtil.class);
    }

    private static Map<String, BeanDefinition> listClientDefinitions(ConfigurableListableBeanFactory beanFactory) {
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

    private static Map<String, BeanDefinition> listChannelDefinitions(ConfigurableListableBeanFactory beanFactory) {
        var beanDefinitions = new HashMap<String, BeanDefinition>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            // Only include channel beans created by the framework
            if (!name.startsWith(GrpcClientUtil.CHANNEL_BEAN_NAME_PREFIX)) {
                continue;
            }
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
            Class<?> clz = beanDefinition.getResolvableType().resolve();
            if (clz != null && ManagedChannel.class.isAssignableFrom(clz)) {
                beanDefinitions.put(name, beanDefinition);
            }
        }
        return beanDefinitions;
    }

    private static boolean isManagedChannel(RegisteredBean registeredBean) {
        return ManagedChannel.class.isAssignableFrom(registeredBean.getBeanClass())
                && isCreatedByFramework(registeredBean);
    }

    private static boolean isGrpcClient(RegisteredBean registeredBean) {
        return AbstractStub.class.isAssignableFrom(registeredBean.getBeanClass())
                && isCreatedByFramework(registeredBean);
    }

    private static boolean isCreatedByFramework(RegisteredBean registeredBean) {
        return registeredBean.getMergedBeanDefinition().hasAttribute(GrpcClientUtil.IS_CREATED_BY_FRAMEWORK);
    }
}
