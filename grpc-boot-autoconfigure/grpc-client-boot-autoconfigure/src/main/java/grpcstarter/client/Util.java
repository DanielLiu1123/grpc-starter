package grpcstarter.client;

import io.grpc.ManagedChannel;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Messy utils for gRPC client module.
 *
 * @author Freeman
 */
final class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    private Util() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Service name field name for gRPC service.
     */
    public static final String SERVICE_NAME = "SERVICE_NAME";

    private static final AntPathMatcher matcher = new AntPathMatcher(".");

    public static List<GrpcClientProperties.Channel> findMatchedConfigs(
            Class<?> stubClass, GrpcClientProperties properties) {
        List<GrpcClientProperties.Channel> matchedChannels = new ArrayList<>();

        for (var channel : properties.getChannels()) {
            boolean matched = false;
            if (matchAnyClassesConfig(stubClass, channel)) {
                matchedChannels.add(channel);
                matched = true;
            }
            if (!matched && matchAnyStubsConfig(stubClass, channel)) {
                matchedChannels.add(channel);
                matched = true;
            }
            if (!matched && matchAnyServicesConfig(stubClass, channel)) {
                matchedChannels.add(channel);
            }
        }

        return matchedChannels;
    }

    private static boolean matchAnyServicesConfig(Class<?> stubClass, GrpcClientProperties.Channel channelConfig) {
        String service = serviceName(stubClass);
        return channelConfig.getServices().stream().anyMatch(svcPattern -> matchPattern(svcPattern, service));
    }

    private static boolean matchAnyStubsConfig(Class<?> stubClass, GrpcClientProperties.Channel channelConfig) {
        return channelConfig.getStubs().stream().anyMatch(stub -> matchStubConfig(stub, stubClass));
    }

    private static boolean matchAnyClassesConfig(Class<?> stubClass, GrpcClientProperties.Channel channelConfig) {
        return channelConfig.getClasses().stream().anyMatch(ch -> ch == stubClass);
    }

    public static boolean matchStubConfig(String stub, Class<?> stubClass) {
        return matchPattern(stub, stubClass.getCanonicalName()) || matchPattern(stub, stubClass.getName());
    }

    public static boolean matchPattern(String pattern, String service) {
        return matcher.match(pattern, service);
    }

    public static String serviceName(Class<?> stubClass) {
        Field serviceNameField = ReflectionUtils.findField(stubClass.getEnclosingClass(), SERVICE_NAME);
        Assert.notNull(serviceNameField, SERVICE_NAME + " field not found");
        Object serviceName = ReflectionUtils.getField(serviceNameField, null);
        Assert.notNull(serviceName, "Service name field value is null");
        return (String) serviceName;
    }

    public static GrpcClientProperties getProperties(Environment environment) {
        GrpcClientProperties properties = Binder.get(environment)
                .bind(GrpcClientProperties.PREFIX, GrpcClientProperties.class)
                .orElseGet(GrpcClientProperties::new);
        properties.afterPropertiesSet();
        return properties;
    }

    public static void shutdownChannel(ManagedChannel channel, Duration timeout) {
        if (channel == null || channel.isTerminated()) {
            return;
        }

        long ms = timeout.toMillis();
        // Close the gRPC managed-channel if not shut down already.
        try {
            channel.shutdown();
            if (!channel.awaitTermination(ms, TimeUnit.MILLISECONDS)) {
                log.warn("Graceful shutdown timed out: {}ms, channel: {}", ms, channel);
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted gracefully shutting down channel: {}", channel);
            Thread.currentThread().interrupt();
        }

        // Forcefully shut down if still not terminated.
        if (!channel.isTerminated()) {
            try {
                channel.shutdownNow();
                if (!channel.awaitTermination(15, TimeUnit.SECONDS)) {
                    log.warn("Forcefully shutdown timed out: 15s, channel: {}. ", channel);
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted forcefully shutting down channel: {}. ", channel);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get class of bean definition.
     *
     * @param beanDefinition bean definition
     * @return class of bean definition
     */
    public static @Nullable Class<?> getBeanDefinitionClass(BeanDefinition beanDefinition) {
        // try to get class from factory method metadata
        // @Configuration + @Bean
        if (beanDefinition instanceof AnnotatedBeanDefinition abd) {
            var metadata = abd.getFactoryMethodMetadata();
            if (metadata != null) {
                // Maybe there has @Conditional on the method,
                // Class may not present.
                return forName(metadata.getReturnTypeName());
            }
        }
        var rt = beanDefinition.getResolvableType();
        if (ResolvableType.NONE.equalsType(rt)) {
            var beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName == null) {
                return null;
            }
            return forName(beanClassName);
        }
        return rt.resolve();
    }

    public static @Nullable Class<?> forName(String beanClassName) {
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static GrpcClientProperties.@Nullable Channel findChannelByName(
            String name, GrpcClientProperties properties) {
        for (var ch : properties.getChannels()) {
            if (Objects.equals(ch.getName(), name)) {
                return ch;
            }
        }

        var defaultChannel = properties.defaultChannel();
        if (Objects.equals(defaultChannel.getName(), name)) {
            return defaultChannel;
        }

        return null;
    }
}
