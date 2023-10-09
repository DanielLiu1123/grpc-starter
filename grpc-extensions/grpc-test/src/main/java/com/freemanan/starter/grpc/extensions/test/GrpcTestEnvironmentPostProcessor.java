package com.freemanan.starter.grpc.extensions.test;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
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

        GrpcTestProperties properties = Binder.get(environment)
                .bind(GrpcTestProperties.PREFIX, GrpcTestProperties.class)
                .orElseGet(GrpcTestProperties::new);

        if (!properties.isEnabled() || !properties.getServer().isEnabled()) {
            return;
        }

        GrpcTestProperties.Server.Port port = properties.getServer().getPort();

        Map<String, Object> configMap = new HashMap<>();
        switch (port) {
            case IN_PROCESS:
                if (GRPC_SERVER_STARTER_EXISTS) {
                    String serverProperty = GrpcServerProperties.InProcess.PREFIX + ".name";
                    if (!environment.containsProperty(serverProperty)) {
                        // use in-process if not manually configured
                        String name = UUID.randomUUID().toString();
                        configMap.put(serverProperty, name);
                    }
                } else {
                    throw new IllegalStateException("gRPC client or server starter not found");
                }
                break;
            case RANDOM_PORT:
                String portProperty = GrpcServerProperties.PREFIX + ".port";
                configMap.put(portProperty, 0);
                break;
            case DEFINED_PORT:
                // do nothing
                break;
            default:
                throw new IllegalArgumentException("Unknown port type: " + port);
        }

        MapPropertySource ps = new MapPropertySource("grpc.extensions.test.property_source", configMap);

        environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, ps);
    }
}
