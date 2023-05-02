package com.freemanan.starter.grpc.client;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
@UtilityClass
class Util {
    /**
     * Service name field name for gRPC service.
     */
    public static final String SERVICE_NAME = "SERVICE_NAME";

    public static Optional<GrpcClientProperties.Channel> findMatchedConfig(
            Class<?> clz, GrpcClientProperties properties) {
        // find from stub class first
        Optional<GrpcClientProperties.Channel> found = properties.getChannels().stream()
                .filter(chan -> chan.getStubs().stream().anyMatch(ch -> ch == clz))
                .findFirst();
        if (found.isPresent()) {
            return found;
        }
        // not class match, try to find from service name
        return properties.getChannels().stream().filter(it -> match(clz, it)).findFirst();
    }

    static boolean match(Class<?> stubClass, GrpcClientProperties.Channel channelConfig) {
        if (channelConfig.getStubs().stream().anyMatch(ch -> ch == stubClass)) {
            return true;
        }
        String service = serviceName(stubClass);
        return channelConfig.getServices().stream().anyMatch(svc -> Objects.equals(svc, service));
    }

    public static String serviceName(Class<?> stubClass) {
        Field serviceNameField = ReflectionUtils.findField(stubClass.getEnclosingClass(), SERVICE_NAME);
        Assert.notNull(serviceNameField, SERVICE_NAME + " field not found");
        return (String) ReflectionUtils.getField(serviceNameField, null);
    }
}
