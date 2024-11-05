package grpcstarter.extensions.validation;

import build.buf.validate.ValidateProto;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Duration;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Empty;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.Struct;
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
import org.projectnessie.cel.common.ULong;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ReflectionUtils;

/**
 * TIP: use 'processAot' task to debug the AOT processing.
 *
 * @author Freeman
 */
class ProtoValidateBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    private final ClassPathScanningCandidateComponentProvider scanner = getScanner();

    @Nullable
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            @Nonnull ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            var reflection = generationContext.getRuntimeHints().reflection();

            // grpcstarter.extensions.validation.ValidationExceptionUtil.asInvalidArgumentException
            registerReflectionForClassAndInnerClasses(reflection, BadRequest.class);

            // This will increase the packaging size about 2MB.
            // I don't know why this type is needed, don't want to spend much time to figure it out :)
            registerReflectionForClassAndInnerClasses(reflection, DescriptorProtos.class);

            // protovalidate
            var protovalidateMessages = scanner.findCandidateComponents(ValidateProto.class.getPackageName());
            for (var clz : protovalidateMessages) {
                try {
                    var clazz = Class.forName(clz.getBeanClassName());
                    registerReflectionForClassAndInnerClasses(reflection, clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            // see org.projectnessie.cel.common.types.pb.Db
            registerReflectionForClassAndInnerClasses(reflection, Any.class);
            registerReflectionForClassAndInnerClasses(reflection, Any[].class);
            registerReflectionForClassAndInnerClasses(reflection, Duration.class);
            registerReflectionForClassAndInnerClasses(reflection, Duration[].class);
            registerReflectionForClassAndInnerClasses(reflection, Empty.class);
            registerReflectionForClassAndInnerClasses(reflection, Empty[].class);
            registerReflectionForClassAndInnerClasses(reflection, Timestamp.class);
            registerReflectionForClassAndInnerClasses(reflection, Timestamp[].class);
            registerReflectionForClassAndInnerClasses(reflection, Value.class);
            registerReflectionForClassAndInnerClasses(reflection, Value[].class);
            registerReflectionForClassAndInnerClasses(reflection, BoolValue.class);
            registerReflectionForClassAndInnerClasses(reflection, BoolValue[].class);

            registerReflectionForClassAndInnerClasses(reflection, Struct.class);
            registerReflectionForClassAndInnerClasses(reflection, Struct[].class);
            registerReflectionForClassAndInnerClasses(reflection, ListValue.class);
            registerReflectionForClassAndInnerClasses(reflection, ListValue[].class);
            registerReflectionForClassAndInnerClasses(reflection, NullValue.class);
            registerReflectionForClassAndInnerClasses(reflection, NullValue[].class);

            registerReflectionForClassAndInnerClasses(reflection, DynamicMessage.class);
            registerReflectionForClassAndInnerClasses(reflection, DynamicMessage[].class);

            registerReflectionForClassAndInnerClasses(reflection, ULong[].class);

            // request + response messages
            var messageClasses = listGrpcServiceDefinition(beanFactory).values().stream()
                    .map(BeanDefinition::getResolvableType)
                    .map(ResolvableType::resolve)
                    .map(ProtoValidateBeanFactoryInitializationAotProcessor::getAllProtobufMessageTypes)
                    .flatMap(Set::stream)
                    .collect(Collectors.toCollection(HashSet::new));

            for (var messageClass : messageClasses) {
                registerReflectionForClassAndInnerClasses(reflection, messageClass);
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

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> isProtobufMessage(metadataReader));
        return scanner;
    }

    private static boolean isProtobufMessage(MetadataReader metadataReader) {
        var classname = metadataReader.getClassMetadata().getClassName();
        try {
            var clz = Class.forName(classname);
            return Message.class.isAssignableFrom(clz)
                    || Message.Builder.class.isAssignableFrom(clz)
                    || ProtocolMessageEnum.class.isAssignableFrom(clz);
        } catch (Throwable e) {
            return false;
        }
    }
}
