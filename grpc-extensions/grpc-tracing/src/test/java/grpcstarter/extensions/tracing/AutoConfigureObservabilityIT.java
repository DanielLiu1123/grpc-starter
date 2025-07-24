package grpcstarter.extensions.tracing;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * This test is to verify that {@link GrpcTracingAutoConfiguration} should not depend on {@link AutoConfigureObservability}.
 */
class AutoConfigureObservabilityIT {

    @Nested
    @SpringBootTest(classes = Cfg.class)
    @AutoConfigureObservability
    class WithAutoConfigureObservability {

        @Autowired
        ApplicationContext ctx;

        @Test
        void shouldHaveObservationGrpcClientInterceptor() {
            assertThatCode(() -> ctx.getBean(ObservationGrpcClientInterceptor.class))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @SpringBootTest(classes = Cfg.class)
    class WithoutAutoConfigureObservability {

        @Autowired
        ApplicationContext ctx;

        @Test
        void shouldHaveObservationGrpcClientInterceptor() {
            assertThatCode(() -> ctx.getBean(ObservationGrpcClientInterceptor.class))
                    .doesNotThrowAnyException();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
