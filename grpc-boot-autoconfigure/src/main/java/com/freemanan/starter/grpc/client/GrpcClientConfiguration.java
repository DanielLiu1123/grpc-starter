package com.freemanan.starter.grpc.client;

import com.freemanan.starter.grpc.GrpcProperties;
import com.freemanan.starter.grpc.client.feature.validation.Validation;
import io.grpc.stub.AbstractStub;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(AbstractStub.class)
@ConditionalOnProperty(prefix = GrpcProperties.Client.PREFIX, name = "enabled", matchIfMissing = true)
@Import({Validation.class})
public class GrpcClientConfiguration implements DisposableBean, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(GrpcClientConfiguration.class);

    private final GrpcProperties.Client properties;

    public GrpcClientConfiguration(GrpcProperties properties) {
        this.properties = properties.getClient();
    }

    @Override
    public void afterSingletonsInstantiated() {
        // Identify the configuration items that doesn't taking effect and print warning messages.
        Set<Class<?>> classes = Cache.getClientClasses();
        properties.getStubs().stream()
                .filter(it -> Util.findMatchedClientClass(it, classes).isEmpty())
                .forEach(it -> log.warn(
                        "Configuration item '{}' doesn't take effect, no matched gRPC stub found, please remove it.",
                        StringUtils.hasText(it.getService())
                                ? "service: " + it.getService()
                                : "stub-class: " + it.getStubClass().getCanonicalName()));
    }

    @Override
    public void destroy() {
        Cache.clear();
    }
}
