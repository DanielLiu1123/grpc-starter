package grpcstarter.extensions.test;

import grpcstarter.server.GrpcServerProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
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
    private static final boolean SPRING_BOOT_TEST_PRESENT =
            ClassUtils.isPresent("org.springframework.boot.test.context.SpringBootTest", null);
    private static final boolean GRPC_SERVER_STARTER_PRESENT =
            ClassUtils.isPresent("grpcstarter.server.GrpcServerProperties", null);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!SPRING_BOOT_TEST_PRESENT || !GRPC_SERVER_STARTER_PRESENT) {
            return;
        }

        Class<?> mainApplicationClass = application.getMainApplicationClass();
        if (mainApplicationClass == null) {
            return;
        }
        SpringBootTest anno = AnnotationUtils.findAnnotation(mainApplicationClass, SpringBootTest.class);
        if (anno == null) {
            return;
        }

        GrpcTestProperties properties = Binder.get(environment)
                .bind(GrpcTestProperties.PREFIX, GrpcTestProperties.class)
                .orElseGet(GrpcTestProperties::new);

        if (!properties.isEnabled() || !properties.getServer().isEnabled()) {
            return;
        }

        GrpcTestProperties.PortType port = properties.getServer().getPortType();

        Map<String, Object> configMap = new HashMap<>();
        switch (port) {
            case IN_PROCESS -> {
                String serverProperty = GrpcServerProperties.InProcess.PREFIX + ".name";
                if (!environment.containsProperty(serverProperty)) {
                    // not manually configured
                    String name = UUID.randomUUID().toString();
                    configMap.put(serverProperty, name);
                }
            }
            case RANDOM_PORT -> {
                String portProperty = GrpcServerProperties.PREFIX + ".port";
                configMap.put(portProperty, 0);
            }
            case DEFINED_PORT -> {
                // do nothing
            }
            case NONE -> {
                // set grpc.server.enabled=false to disable grpc server
                String enabledProperty = GrpcServerProperties.PREFIX + ".enabled";
                configMap.put(enabledProperty, false);
            }
            default -> throw new IllegalArgumentException("Unknown port type: " + port);
        }

        MapPropertySource ps = new MapPropertySource("grpc.extensions.test.property_source", configMap);

        environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, ps);
    }
}
