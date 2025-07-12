package grpcstarter.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import grpcstarter.server.feature.channelz.Channelz;
import grpcstarter.server.feature.exceptionhandling.ExceptionHandling;
import grpcstarter.server.feature.health.Health;
import grpcstarter.server.feature.reflection.Reflection;
import io.grpc.BindableService;
import io.grpc.health.v1.HealthGrpc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Freeman
 */
class GrpcServerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
            .withBean(SslBundles.class, () -> mock(SslBundles.class))
            .withPropertyValues("grpc.server.port=0");

    @Test
    void testAutoConfigurationLoads() {
        this.runner.run(context -> {
            assertThat(context).hasSingleBean(GrpcServerProperties.class);
            assertThat(context).hasSingleBean(GrpcServer.class);
            assertThat(context).hasSingleBean(GrpcRequestContextServerInterceptor.class);
        });
    }

    @Test
    void testAutoConfigurationDisabledWhenGrpcServerDisabled() {
        this.runner.withPropertyValues("grpc.server.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(GrpcServerProperties.class);
            assertThat(context).doesNotHaveBean(GrpcServer.class);
            assertThat(context).doesNotHaveBean(GrpcRequestContextServerInterceptor.class);
        });
    }

    @Test
    void testGrpcServerBeanCreatedByDefault() {
        this.runner.run(context -> {
            assertThat(context).hasSingleBean(GrpcServer.class);
            GrpcServer grpcServer = context.getBean(GrpcServer.class);
            // Default should be DefaultGrpcServer when enableEmptyServer is true (default)
            assertThat(grpcServer).isInstanceOf(DefaultGrpcServer.class);
        });
    }

    @Test
    void testDefaultGrpcServerWhenEmptyServerEnabled() {
        this.runner.withPropertyValues("grpc.server.enable-empty-server=true").run(context -> {
            assertThat(context).hasSingleBean(GrpcServer.class);
            GrpcServer grpcServer = context.getBean(GrpcServer.class);
            assertThat(grpcServer).isInstanceOf(DefaultGrpcServer.class);
        });
    }

    @Test
    void testDummyGrpcServerWhenEmptyServerDisabledAndOnlyInternalServices() {
        this.runner
                .withPropertyValues("grpc.server.enable-empty-server=false")
                .withUserConfiguration(InternalServiceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(GrpcServer.class);
                    GrpcServer grpcServer = context.getBean(GrpcServer.class);
                    assertThat(grpcServer).isInstanceOf(DummyGrpcServer.class);
                });
    }

    @Test
    void testDefaultGrpcServerWhenHasUserServices() {
        this.runner
                .withPropertyValues("grpc.server.enable-empty-server=false")
                .withUserConfiguration(UserServiceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(GrpcServer.class);
                    GrpcServer grpcServer = context.getBean(GrpcServer.class);
                    assertThat(grpcServer).isInstanceOf(DefaultGrpcServer.class);
                });
    }

    @Test
    void testGrpcRequestContextServerInterceptorCreated() {
        this.runner.run(context -> {
            assertThat(context).hasSingleBean(GrpcRequestContextServerInterceptor.class);
            var interceptor = context.getBean(GrpcRequestContextServerInterceptor.class);
            assertThat(interceptor).isNotNull();
        });
    }

    @Test
    void testCustomGrpcRequestContextServerInterceptorNotOverridden() {
        this.runner.withUserConfiguration(CustomInterceptorConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(GrpcRequestContextServerInterceptor.class);
            var interceptor = context.getBean(GrpcRequestContextServerInterceptor.class);
            assertThat(interceptor).isInstanceOf(TestGrpcRequestContextServerInterceptor.class);
        });
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testVirtualThreadCustomizerCreatedWhenVirtualThreadsEnabledAndOnJavaGreater21() {
        this.runner.withPropertyValues("spring.threads.virtual.enabled=true").run(context -> {
            assertThat(context).hasBean("virtualThreadGrpcServerCustomizer");
            assertThat(context.getBean("virtualThreadGrpcServerCustomizer"))
                    .isInstanceOf(VirtualThreadGrpcServerCustomizer.class);
        });
    }

    @Test
    @EnabledForJreRange(max = JRE.JAVA_21)
    void testVirtualThreadCustomizerNotCreatedWhenVirtualThreadsEnabledAndOnJavaLess21() {
        this.runner.withPropertyValues("spring.threads.virtual.enabled=true").run(context -> {
            assertThat(context).doesNotHaveBean("virtualThreadGrpcServerCustomizer");
        });
    }

    @Test
    void testVirtualThreadCustomizerNotCreatedWhenVirtualThreadsDisabled() {
        this.runner.withPropertyValues("spring.threads.virtual.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean("virtualThreadGrpcServerCustomizer");
        });
    }

    @Test
    void testVirtualThreadCustomizerNotCreatedByDefault() {
        this.runner.run(context -> {
            assertThat(context).doesNotHaveBean("virtualThreadGrpcServerCustomizer");
        });
    }

    @Test
    void testFeaturesConfigurationImported() {
        this.runner.run(context -> {
            assertThat(context).doesNotHaveBean(Reflection.class);
            assertThat(context).hasSingleBean(Health.class);
            assertThat(context).doesNotHaveBean(Channelz.class);
            assertThat(context).hasSingleBean(ExceptionHandling.class);
        });
    }

    @Test
    void testGrpcServerWithInProcessConfiguration() {
        this.runner
                .withPropertyValues("grpc.server.in-process.name=test-server")
                .run(context -> {
                    assertThat(context).hasSingleBean(GrpcServer.class);
                    GrpcServer grpcServer = context.getBean(GrpcServer.class);
                    assertThat(grpcServer.getPort()).isEqualTo(-1);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class InternalServiceConfiguration {
        @Bean
        public BindableService healthService() {
            return new HealthGrpc.HealthImplBase() {};
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UserServiceConfiguration {
        @Bean
        public BindableService testUserService() {
            return new TestUserService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomInterceptorConfiguration {
        @Bean
        public GrpcRequestContextServerInterceptor grpcRequestContextServerInterceptor() {
            return new TestGrpcRequestContextServerInterceptor();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MultipleServicesConfiguration {
        @Bean
        public BindableService testUserService() {
            return new TestUserService();
        }

        @Bean
        public BindableService anotherTestService() {
            return new AnotherTestService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomizersConfiguration {
        @Bean
        public GrpcServerCustomizer testCustomizer() {
            return new TestGrpcServerCustomizer();
        }
    }

    static class TestUserService implements BindableService {
        @Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder("test.UserService").build();
        }
    }

    static class AnotherTestService implements BindableService {
        @Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder("test.AnotherService")
                    .build();
        }
    }

    static class TestGrpcRequestContextServerInterceptor extends GrpcRequestContextServerInterceptor {
        public TestGrpcRequestContextServerInterceptor() {
            super(new GrpcServerProperties());
        }
    }

    static class TestGrpcServerCustomizer implements GrpcServerCustomizer {
        @Override
        public void customize(io.grpc.ServerBuilder<?> serverBuilder) {
            // Test customization
        }
    }
}
