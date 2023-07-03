package com.freemanan.starter.grpc.server;

import io.grpc.BindableService;
import io.grpc.channelz.v1.ChannelzGrpc;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.aop.framework.AopProxyUtils;

/**
 * @author Freeman
 */
@UtilityClass
class Util {

    private static final Set<Class<? extends BindableService>> internalServiceClasses = getInternalServiceClasses();

    private static Set<Class<? extends BindableService>> getInternalServiceClasses() {
        Set<Class<? extends BindableService>> result = new HashSet<>();
        result.add(ServerReflectionGrpc.ServerReflectionImplBase.class);
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
