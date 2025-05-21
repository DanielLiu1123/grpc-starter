package grpcstarter.extensions.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import cr.Classpath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * {@link GrpcTracingAutoConfiguration} tester.
 */
class GrpcTracingAutoConfigurationTest {

    static ApplicationContextRunner runner = new ApplicationContextRunner();

    @Test
    void testTracingBeans_whenAllConditionsMatched() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .run()) {

            assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Server.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(OrderedObservationGrpcServerInterceptor.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Client.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(OrderedObservationGrpcClientInterceptor.class))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void tracingNotEnabled_whenManagementTracingNotEnabled() {
        runner.withConfiguration(AutoConfigurations.of(GrpcTracingAutoConfiguration.class))
                .withPropertyValues("management.tracing.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GrpcTracingAutoConfiguration.class));
    }

    @Test
    @Classpath(exclude = "grpc-server-boot-autoconfigure-*.jar")
    void tracingServerNotEnabled_whenGrpcServerNotOnClasspath() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class).run()) {

            assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Server.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @Classpath(exclude = "grpc-client-boot-autoconfigure-*.jar")
    void tracingClientNotEnabled_whenGrpcClientNotOnClasspath() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .run()) {

            assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Client.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
