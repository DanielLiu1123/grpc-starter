package grpcstarter.server;

import io.grpc.BindableService;
import io.grpc.channelz.v1.ChannelzGrpc;
import io.grpc.health.v1.HealthGrpc;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.aop.framework.AopProxyUtils;

/**
 * @author Freeman
 */
@UtilityClass
class Util {

    private static final Set<Class<?>> internalServiceClasses = getInternalServiceClasses();

    private static Set<Class<?>> getInternalServiceClasses() {
        Set<Class<?>> result = new HashSet<>();

        try {
            // legacy Reflection
            Class<?> clz = Class.forName("io.grpc.reflection.v1alpha.ServerReflectionGrpc$ServerReflectionImplBase");
            result.add(clz);
        } catch (ClassNotFoundException ignored) {
        }

        try {
            // v1 Reflection
            Class<?> clz = Class.forName("io.grpc.reflection.v1.ServerReflectionGrpc$ServerReflectionImplBase");
            result.add(clz);
        } catch (ClassNotFoundException ignored) {
        }

        result.add(HealthGrpc.HealthImplBase.class);
        result.add(ChannelzGrpc.ChannelzImplBase.class);
        return result;
    }

    public static boolean isInternalService(Class<?> clz) {
        return internalServiceClasses.stream().anyMatch(clazz -> clazz.isAssignableFrom(clz));
    }

    public static boolean allInternalServices(Set<BindableService> services) {
        return services.stream().map(AopProxyUtils::ultimateTargetClass).allMatch(Util::isInternalService);
    }
}
