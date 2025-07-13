package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for virtual threads support in gRPC client.
 *
 * @author Freeman
 */
class VirtualThreadsIT {

    @Test
    void testVirtualThreadsEnabled() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.authority=localhost:9090")
                .properties("spring.threads.virtual.enabled=true")
                .run()) {

            // Verify that the virtual thread customizer bean is created
            assertThatCode(() -> ctx.getBean("virtualThreadGrpcChannelCustomizer"))
                    .doesNotThrowAnyException();

            // Verify that it's the correct type
            Object customizer = ctx.getBean("virtualThreadGrpcChannelCustomizer");
            assertThat(customizer).isInstanceOf(VirtualThreadGrpcChannelCustomizer.class);

            // Verify that the gRPC client properties are still created successfully
            assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        }
    }

    @Test
    void testVirtualThreadsDisabled() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.authority=localhost:9090")
                .properties("spring.threads.virtual.enabled=false")
                .run()) {

            // Verify that the virtual thread customizer bean is NOT created
            assertThatCode(() -> ctx.getBean("virtualThreadGrpcChannelCustomizer"))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);

            // Verify that the gRPC client properties are still created successfully
            assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        }
    }

    @Test
    void testVirtualThreadsNotConfigured() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.authority=localhost:9090")
                .run()) {

            // Verify that the virtual thread customizer bean is NOT created when property is not set
            assertThatCode(() -> ctx.getBean("virtualThreadGrpcChannelCustomizer"))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);

            // Verify that the gRPC client properties are still created successfully
            assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        }
    }

    @Test
    void testVirtualThreadsWithInProcessClient() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.in-process.name=test-client")
                .properties("spring.threads.virtual.enabled=true")
                .run()) {

            // Verify that the virtual thread customizer bean is created
            assertThatCode(() -> ctx.getBean("virtualThreadGrpcChannelCustomizer"))
                    .doesNotThrowAnyException();

            // Verify that the gRPC client properties are still created successfully
            assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
