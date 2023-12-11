package com.freemanan.starter.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link GrpcClientAutoConfiguration} tester.
 */
class GrpcClientAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GrpcClientAutoConfiguration.class));

    @Test
    void testDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GrpcStubBeanDefinitionRegistry.class);
            assertThat(context).hasSingleBean(GrpcClientOptionsClientInterceptor.class);
            assertThat(context).getBean("grpcClientUnusedConfigChecker").isInstanceOf(CommandLineRunner.class);
            assertThat(context).hasSingleBean(ShutdownEventBasedChannelCloser.class);

            assertThat(context).doesNotHaveBean(GrpcClientRefreshScopeRefreshedEventListener.class);
        });
    }

    @Test
    void testClientEnabled() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GrpcClientAutoConfiguration.class);
        });

        runner.withPropertyValues("grpc.client.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(GrpcClientAutoConfiguration.class);
            assertThat(context).doesNotHaveBean(GrpcClientProperties.class);
        });
    }

    @Test
    void whenWarnUnusedConfigDisabled_thenNoUnusedConfigCheckerBean() {
        runner.withPropertyValues("grpc.client.warn-unused-config-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("grpcClientUnusedConfigChecker");
                });
    }
}
