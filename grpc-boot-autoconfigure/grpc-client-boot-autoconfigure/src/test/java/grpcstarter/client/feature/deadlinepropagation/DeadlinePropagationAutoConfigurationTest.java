package grpcstarter.client.feature.deadlinepropagation;

import static org.assertj.core.api.Assertions.assertThat;

import grpcstarter.client.GrpcClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for {@link GrpcClientDeadlinePropagationAutoConfiguration}.
 */
class DeadlinePropagationAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GrpcClientAutoConfiguration.class, GrpcClientDeadlinePropagationAutoConfiguration.class));

    @Test
    void testBeanPresentByDefault() {
        runner.run(context -> assertThat(context).hasSingleBean(DeadlinePropagationClientInterceptor.class));
    }

    @Test
    void testBeanAbsentWhenDeadlinePropagationDisabled() {
        runner.withPropertyValues("grpc.client.deadline-propagation.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DeadlinePropagationClientInterceptor.class));
    }

    @Test
    void testBeanAbsentWhenGrpcClientDisabled() {
        runner.withPropertyValues("grpc.client.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DeadlinePropagationClientInterceptor.class));
    }
}
