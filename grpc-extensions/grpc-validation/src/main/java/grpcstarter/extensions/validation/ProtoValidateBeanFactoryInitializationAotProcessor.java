package grpcstarter.extensions.validation;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.rpc.BadRequest;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
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
class ProtoValidateBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Nullable
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            @Nonnull ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            var reflection = generationContext.getRuntimeHints().reflection();

            // grpcstarter.extensions.validation.ValidationExceptionUtil.asInvalidArgumentException
            registerReflectionForClassAndInnerClasses(reflection, BadRequest.class);

            // see org.projectnessie.cel.common.types.pb.Db
            registerReflectionForClassAndInnerClasses(reflection, Any.class);
            registerReflectionForClassAndInnerClasses(reflection, Duration.class);
            registerReflectionForClassAndInnerClasses(reflection, Empty.class);
            registerReflectionForClassAndInnerClasses(reflection, Timestamp.class);
            registerReflectionForClassAndInnerClasses(reflection, Value.class);
            registerReflectionForClassAndInnerClasses(reflection, BoolValue.class);

            // request + response messages
            var messageClasses = listGrpcServiceDefinition(beanFactory).values().stream()
                    .map(BeanDefinition::getResolvableType)
                    .map(ResolvableType::resolve)
                    .map(ProtoValidateBeanFactoryInitializationAotProcessor::getAllProtobufMessageTypes)
                    .flatMap(Set::stream)
                    .collect(Collectors.toCollection(HashSet::new));

            for (var messageClass : messageClasses) {
                reflection.registerType(
                        messageClass, MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS);
            }
        };
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

        reflection.registerType(clz, MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS);

        for (var declaredClass : clz.getDeclaredClasses()) {
            registerReflectionForClassAndInnerClasses(reflection, declaredClass);
        }
    }
}
