package grpcstarter.extensions.transcoding;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ClassUtils;

/**
 * TIP: use 'processAot' task to debug the AOT processing.
 *
 * @author Freeman
 */
class GrpcTranscodingBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    private final Environment env;

    GrpcTranscodingBeanFactoryInitializationAotProcessor(Environment env) {
        this.env = env;
    }

    @Nullable
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            var enabled = env.getProperty(GrpcTranscodingProperties.PREFIX + ".enabled", Boolean.class, true);
            if (!enabled) {
                return;
            }

            var reflection = generationContext.getRuntimeHints().reflection();

            // See grpcstarter.extensions.transcoding.DefaultHeaderConverter.getHttpHeaders
            reflection.registerType(
                    HttpHeaders.class, builder -> builder.withMembers(MemberCategory.ACCESS_PUBLIC_FIELDS));

            // This will increase the packaging size about 2MB.
            // I don't know why this type is needed, don't want to spend much time to figure it out :)
            registerReflectionForClassAndInnerClasses(reflection, DescriptorProtos.class);

            // request + response messages
            registerReflectionForMessages(reflection, getMessages(listGrpcServiceDefinition(beanFactory)));
        };
    }

    private static void registerReflectionForClassAndInnerClasses(ReflectionHints reflection, Class<?> clz) {

        reflection.registerType(clz, MemberCategory.INVOKE_PUBLIC_METHODS);

        for (var declaredClass : clz.getDeclaredClasses()) {
            registerReflectionForClassAndInnerClasses(reflection, declaredClass);
        }
    }

    private static Set<Class<?>> getMessages(Map<String, BeanDefinition> beanNameToBeanDefinition) {
        var messages = new LinkedHashSet<Class<?>>();
        for (var entry : beanNameToBeanDefinition.entrySet()) {
            var beanDefinition = entry.getValue();
            var clz = beanDefinition.getResolvableType().resolve();
            if (clz == null) {
                continue;
            }

            var methods = clz.getMethods();
            for (var method : methods) {
                Class<?> returnType = method.getReturnType();
                if (returnType != void.class) { // grpc method should return void
                    continue;
                }
                if (method.getParameterCount() != 2) { // grpc method should have 2 parameters
                    continue;
                }
                Class<?> message1 = method.getParameterTypes()[0];
                if (!Message.class.isAssignableFrom(message1)) { // the first parameter should be a Message
                    continue;
                }
                Type arg2 = method.getGenericParameterTypes()[1]; // the second parameter must be a StreamObserver
                if (!(arg2 instanceof ParameterizedType pt)) {
                    continue;
                }
                var typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length != 1) {
                    continue;
                }
                if (!(typeArgs[0] instanceof Class<?> message2)) {
                    continue;
                }
                if (!Message.class.isAssignableFrom(message2)) { // the second parameter should be a Message
                    continue;
                }

                messages.add(message1);
                messages.add(message2);
            }
        }
        return messages;
    }

    private static void registerReflectionForMessages(ReflectionHints reflection, Set<Class<?>> messages) {
        for (var message : messages) {

            // register the message and its builder
            reflection.registerType(message, MemberCategory.INVOKE_PUBLIC_METHODS);

            var builderClass = getBuilderClass(message);
            if (builderClass != null) {
                reflection.registerType(builderClass, MemberCategory.INVOKE_PUBLIC_METHODS);
            }
        }
    }

    @Nullable
    private static Class<?> getBuilderClass(Class<?> message) {
        try {
            return ClassUtils.forName(message.getName() + "$Builder", null);
        } catch (ClassNotFoundException e) {
            return null;
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
}
