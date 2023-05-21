package com.freemanan.starter.grpc.extensions.test;

import com.freemanan.starter.grpc.client.GrpcClientProperties;
import com.freemanan.starter.grpc.server.GrpcServerProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;

/**
 * gRPC integrate with {@link SpringBootTest}.
 *
 * @author Freeman
 */
public class GrpcTestEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final boolean SPRING_BOOT_TEST_ENV =
            ClassUtils.isPresent("org.springframework.boot.test.context.SpringBootTest", null);
    private static final boolean GRPC_CLIENT_STARTER_EXISTS =
            ClassUtils.isPresent("com.freemanan.starter.grpc.client.GrpcClientProperties", null);
    private static final boolean GRPC_SERVER_STARTER_EXISTS =
            ClassUtils.isPresent("com.freemanan.starter.grpc.server.GrpcServerProperties", null);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!SPRING_BOOT_TEST_ENV) {
            return;
        }

        SpringBootTest anno =
                AnnotationUtils.findAnnotation(application.getMainApplicationClass(), SpringBootTest.class);
        if (anno == null) {
            return;
        }

        Map<String, Object> configMap = new HashMap<>();

        startGrpcWithInProcessIfNecessary(environment, configMap);

        MapPropertySource ps = new MapPropertySource("grpc.extensions.test.property_source", configMap);

        environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, ps);
    }

    private static void startGrpcWithInProcessIfNecessary(
            ConfigurableEnvironment environment, Map<String, Object> configMap) {
        if (GRPC_CLIENT_STARTER_EXISTS && GRPC_SERVER_STARTER_EXISTS) {
            String serverProperty = GrpcServerProperties.InProcess.PREFIX + ".name";
            String clientProperty = GrpcClientProperties.InProcess.PREFIX + ".name";
            if (!environment.containsProperty(serverProperty) && !environment.containsProperty(clientProperty)) {
                // use in-process if not manually configured
                String name = UUID.randomUUID().toString();
                configMap.put(serverProperty, name);
                configMap.put(clientProperty, name);
            }
        } else if (GRPC_SERVER_STARTER_EXISTS) {
            // use random port
            String portProperty = GrpcServerProperties.PREFIX + ".port";
            configMap.put(portProperty, 0);
        }
    }
}
