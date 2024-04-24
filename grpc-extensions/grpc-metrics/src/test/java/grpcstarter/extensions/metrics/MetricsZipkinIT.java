package grpcstarter.extensions.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = MetricsZipkinIT.Cfg.class)
@Testcontainers(disabledWithoutDocker = true)
class MetricsZipkinIT {

    @Container
    static GenericContainer<?> zipkin = new GenericContainer<>("openzipkin/zipkin:latest").withExposedPorts(9411);

    @DynamicPropertySource
    static void zipkinProperties(DynamicPropertyRegistry registry) {
        registry.add("management.zipkin.tracing.endpoint", () -> "http://localhost:" + zipkin.getFirstMappedPort());
    }

    @Test
    void testZipkin() {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
