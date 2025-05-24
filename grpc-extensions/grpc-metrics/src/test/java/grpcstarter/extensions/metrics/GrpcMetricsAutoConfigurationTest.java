package grpcstarter.extensions.metrics;

import static grpcstarter.extensions.metrics.Deps.SPRING_BOOT_VERSION;
import static org.assertj.core.api.Assertions.assertThatCode;

import cr.Classpath;
import grpcstarter.server.GrpcServerProperties;
import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.observation.aop.ObservedAspect;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * {@link GrpcMetricsAutoConfiguration} tester.
 */
class GrpcMetricsAutoConfigurationTest {

    @Test
    @Classpath(add = "org.springframework.boot:spring-boot-starter-aop:" + SPRING_BOOT_VERSION)
    void testMetricsBeans_whenAllConditionsMatched() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".in-process.name=" + UUID.randomUUID())
                // From 3.2.2, see https://github.com/spring-projects/spring-boot/issues/39128
                .properties("management.observations.annotations.enabled=true")
                .run();

        assertThatCode(() -> ctx.getBean(OrderedMetricCollectingServerInterceptor.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(OrderedMetricCollectingClientInterceptor.class))
                .doesNotThrowAnyException();

        assertThatCode(() -> ctx.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME))
                .doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(AnnotationAwareAspectJAutoProxyCreator.class))
                .doesNotThrowAnyException();
        // MetricsAspectsAutoConfiguration add CountedAspect, TimedAspect and ObservedAspect beans from 3.2.0
        assertThatCode(() -> ctx.getBean(CountedAspect.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(TimedAspect.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(ObservedAspect.class)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    @Classpath(exclude = "grpc-server-boot-autoconfigure-*.jar")
    void testMetricsBeans_whenGrpcServerNotOnClasspath() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class).run();

        assertThatCode(() -> ctx.getBean(OrderedMetricCollectingServerInterceptor.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatCode(() -> ctx.getBean(OrderedMetricCollectingClientInterceptor.class))
                .doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    @Classpath(exclude = "grpc-client-boot-autoconfigure-*.jar")
    void testMetricsBeans_whenGrpcClientNotOnClasspath() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".in-process.name=" + UUID.randomUUID())
                .run();

        assertThatCode(() -> ctx.getBean(OrderedMetricCollectingServerInterceptor.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(OrderedMetricCollectingClientInterceptor.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
