package grpcstarter.extensions.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.cr.core.anno.Verb;
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
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .run();

        assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Server.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(OrderedObservationGrpcServerInterceptor.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Client.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(OrderedObservationGrpcClientInterceptor.class))
                .doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void tracingNotEnabled_whenManagementTracingNotEnabled() {
        runner.withConfiguration(AutoConfigurations.of(GrpcTracingAutoConfiguration.class))
                .withPropertyValues("management.tracing.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GrpcTracingAutoConfiguration.class));
    }

    @Test
    @ClasspathReplacer({
        @Action(verb = Verb.EXCLUDE, value = "grpc-server-boot-autoconfigure-*.jar"),
    })
    void tracingServerNotEnabled_whenGrpcServerNotOnClasspath() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class).run();

        assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Server.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    @ClasspathReplacer({
        @Action(verb = Verb.EXCLUDE, value = "grpc-client-boot-autoconfigure-*.jar"),
    })
    void tracingClientNotEnabled_whenGrpcClientNotOnClasspath() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .run();

        assertThatCode(() -> ctx.getBean(GrpcTracingAutoConfiguration.Client.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
