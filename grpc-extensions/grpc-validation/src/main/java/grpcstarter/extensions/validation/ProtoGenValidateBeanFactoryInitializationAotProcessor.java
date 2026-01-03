package grpcstarter.extensions.validation;

import com.google.protobuf.Message;
import com.google.rpc.BadRequest;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

/**
 * TIP: use 'processAot' task to debug the AOT processing.
 *
 * @author Freeman
 */
class ProtoGenValidateBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
            ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            var reflection = generationContext.getRuntimeHints().reflection();

            // io.envoyproxy.pgv.grpc.ValidationExceptions.asStatusRuntimeException
            registerReflectionForClassAndInnerClasses(reflection, BadRequest.class);

            // request + response messages
            var messageClasses = listGrpcServiceDefinition(beanFactory).values().stream()
                    .map(BeanDefinition::getResolvableType)
                    .map(ResolvableType::resolve)
                    .map(ProtoGenValidateBeanFactoryInitializationAotProcessor::getAllProtobufMessageTypes)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            for (var messageClass : messageClasses) {
                reflection.registerTypeIfPresent(
                        null,
                        getEnclosingClass(messageClass).getName() + "Validator",
                        builder -> builder.withMethod(
                                "validatorFor", List.of(TypeReference.of(Class.class)), ExecutableMode.INVOKE));
            }
        };
    }

    private static Class<?> getEnclosingClass(Class<?> messageClass) {
        var enclosingClass = messageClass;
        while (enclosingClass.getEnclosingClass() != null) {
            enclosingClass = enclosingClass.getEnclosingClass();
        }
        return enclosingClass;
    }

    public static Set<Class<?>> getAllProtobufMessageTypes(Class<?> grpcServiceClass) {
        var messageTypes = new HashSet<Class<?>>();

        ReflectionUtils.doWithMethods(
                grpcServiceClass,
                method -> {
                    Type[] parameterTypes = method.getGenericParameterTypes();
                    for (Type type : parameterTypes) {
                        collectMessageTypesFromType(type, messageTypes);
                    }

                    Type returnType = method.getGenericReturnType();
                    collectMessageTypesFromType(returnType, messageTypes);
                },
                method -> Modifier.isPublic(method.getModifiers())
                        && (method.getReturnType() == void.class || method.getReturnType() == StreamObserver.class));

        return messageTypes;
    }

    @SuppressWarnings("NonApiType")
    private static void collectMessageTypesFromType(Type type, HashSet<Class<?>> messageTypes) {
        if (type instanceof Class<?> clazz) {
            if (Message.class.isAssignableFrom(clazz)) {
                messageTypes.add(clazz);
            }
        } else if (type instanceof ParameterizedType paramType) {
            collectMessageTypesFromType(paramType.getRawType(), messageTypes);

            for (Type argType : paramType.getActualTypeArguments()) {
                collectMessageTypesFromType(argType, messageTypes);
            }
        }
    }

    private static Map<String, BeanDefinition> listGrpcServiceDefinition(ConfigurableListableBeanFactory beanFactory) {
        var beanDefinitions = new HashMap<String, BeanDefinition>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
            Class<?> clz = beanDefinition.getResolvableType().resolve();
            if (clz != null && BindableService.class.isAssignableFrom(clz)) {
                beanDefinitions.put(name, beanDefinition);
            }
        }
        return beanDefinitions;
    }

    private static void registerReflectionForClassAndInnerClasses(ReflectionHints reflection, Class<?> clz) {

        reflection.registerType(clz, MemberCategory.INVOKE_PUBLIC_METHODS);

        for (var declaredClass : clz.getDeclaredClasses()) {
            registerReflectionForClassAndInnerClasses(reflection, declaredClass);
        }
    }
}
