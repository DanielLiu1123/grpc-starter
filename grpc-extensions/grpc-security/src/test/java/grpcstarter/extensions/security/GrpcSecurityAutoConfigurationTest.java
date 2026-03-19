package grpcstarter.extensions.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

/**
 * Tests for {@link GrpcSecurityAutoConfiguration}.
 */
class GrpcSecurityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GrpcSecurityAutoConfiguration.class));

    @Test
    void testAuthenticationReaderRegisteredByDefault() {
        runner.run(context -> assertThat(context).hasSingleBean(GrpcAuthenticationReader.class));
    }

    @Test
    void testInterceptorRegisteredWhenAuthenticationManagerPresent() {
        runner.withUserConfiguration(AuthManagerConfig.class)
                .run(context -> assertThat(context).hasSingleBean(GrpcSecurityServerInterceptor.class));
    }

    @Test
    void testInterceptorNotRegisteredWithoutAuthenticationManager() {
        runner.run(context -> assertThat(context).doesNotHaveBean(GrpcSecurityServerInterceptor.class));
    }

    @Test
    void testAllDisabledWhenGrpcSecurityDisabled() {
        runner.withPropertyValues("grpc.security.enabled=false")
                .withUserConfiguration(AuthManagerConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrpcAuthenticationReader.class);
                    assertThat(context).doesNotHaveBean(GrpcSecurityServerInterceptor.class);
                });
    }

    @Test
    void testInterceptorNotRegisteredWhenServerSecurityDisabled() {
        runner.withPropertyValues("grpc.security.server.enabled=false")
                .withUserConfiguration(AuthManagerConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(GrpcSecurityServerInterceptor.class));
    }

    @Test
    void testCustomAuthenticationReaderHonored() {
        runner.withUserConfiguration(CustomReaderConfig.class).run(context -> {
            assertThat(context).hasSingleBean(GrpcAuthenticationReader.class);
            assertThat(context.getBean(GrpcAuthenticationReader.class)).isInstanceOf(CustomReaderConfig.MyReader.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class AuthManagerConfig {
        @Bean
        AuthenticationManager authenticationManager() {
            return mock(AuthenticationManager.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomReaderConfig {
        static class MyReader implements GrpcAuthenticationReader {
            @Override
            public @Nullable Authentication readAuthentication(ServerCall<?, ?> call, Metadata headers) {
                return null;
            }
        }

        @Bean
        GrpcAuthenticationReader grpcAuthenticationReader() {
            return new MyReader();
        }
    }
}
