package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.starter.grpc.server.feature.exceptionhandling.annotation.AnnotationBasedGrpcExceptionResolver;
import com.freemanan.starter.grpc.server.feature.exceptionhandling.annotation.DefaultGrpcExceptionAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link ExceptionHandling} tester.
 */
class ExceptionHandlingTests {

    final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ExceptionHandling.class);

    @Test
    void testDefaultBehavior() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AnnotationBasedGrpcExceptionResolver.class);
            assertThat(context).hasSingleBean(ExceptionHandlingServerInterceptor.class);
            assertThat(context).hasSingleBean(DefaultGrpcExceptionAdvice.class);
        });
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("grpc.server.exception-handling.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AnnotationBasedGrpcExceptionResolver.class);
                    assertThat(context).doesNotHaveBean(ExceptionHandlingServerInterceptor.class);
                    assertThat(context).doesNotHaveBean(DefaultGrpcExceptionAdvice.class);
                });
    }

    @Test
    void testDefaultGrpcExceptionAdviceDisabled() {
        runner.withPropertyValues("grpc.server.exception-handling.default-exception-advice-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AnnotationBasedGrpcExceptionResolver.class);
                    assertThat(context).hasSingleBean(ExceptionHandlingServerInterceptor.class);
                    assertThat(context).doesNotHaveBean(DefaultGrpcExceptionAdvice.class);
                });
    }
}
