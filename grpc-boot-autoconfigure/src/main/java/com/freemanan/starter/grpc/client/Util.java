package com.freemanan.starter.grpc.client;

import com.freemanan.starter.grpc.GrpcProperties;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
@UtilityClass
class Util {

    public static Optional<GrpcProperties.Client.Stub> findMatchedConfig(
            Class<?> clz, GrpcProperties.Client properties) {
        // find from all clientClass first
        Optional<GrpcProperties.Client.Stub> found = properties.getStubs().stream()
                .filter(it -> it.getStubClass() == clz)
                .findFirst();
        if (found.isPresent()) {
            return found;
        }
        // not class match, try to find from the normal way
        return properties.getStubs().stream().filter(it -> match(clz, it)).findFirst();
    }

    public static Optional<Class<?>> findMatchedClientClass(GrpcProperties.Client.Stub stub, Set<Class<?>> classes) {
        // directly find from the normal way, the found one may not correct
        return classes.stream().filter(clz -> match(clz, stub)).findFirst();
    }

    static boolean match(Class<?> clz, GrpcProperties.Client.Stub stub) {
        if (clz == stub.getStubClass()) {
            return true;
        }
        if (!StringUtils.hasText(stub.getService())) {
            return false;
        }
        Field serviceNameField = ReflectionUtils.findField(clz.getEnclosingClass(), "SERVICE_NAME");
        Assert.notNull(serviceNameField, "SERVICE_NAME field not found");
        String service = (String) ReflectionUtils.getField(serviceNameField, null);
        return stub.getService().equalsIgnoreCase(service);
    }
}
