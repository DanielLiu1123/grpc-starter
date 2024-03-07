package com.freemanan.starter.grpc.extensions.validation;

import static com.freemanan.cr.core.anno.Verb.EXCLUDE;
import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import io.envoyproxy.pgv.grpc.ValidatingClientInterceptor;
import io.envoyproxy.pgv.grpc.ValidatingServerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link GrpcValidationAutoConfiguration} tester.
 */
class GrpcValidationAutoConfigurationTest {

    @Test
    void haveAllBeans_whenAllInClasspath() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GrpcValidationAutoConfiguration.class));

        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ValidatingClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ValidatingServerInterceptor.class);
            assertThat(ctx).hasSingleBean(ProtoValidateClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ProtoValidateServerInterceptor.class);
        });
    }

    @Test
    void notHavePgvRelatedBeans_whenConfigureBackendToProtoValidate() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GrpcValidationAutoConfiguration.class));

        runner.withPropertyValues("grpc.validation.backend=PROTO_VALIDATE").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ValidatingClientInterceptor.class);
            assertThat(ctx).doesNotHaveBean(ValidatingServerInterceptor.class);
            assertThat(ctx).hasSingleBean(ProtoValidateClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ProtoValidateServerInterceptor.class);
        });

        runner.withPropertyValues("grpc.validation.backend=proto_validate").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ValidatingClientInterceptor.class);
            assertThat(ctx).doesNotHaveBean(ValidatingServerInterceptor.class);
            assertThat(ctx).hasSingleBean(ProtoValidateClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ProtoValidateServerInterceptor.class);
        });
    }

    @Test
    void notHaveProtoValidateRelatedBeans_whenConfigureBackendToPgv() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GrpcValidationAutoConfiguration.class));

        runner.withPropertyValues("grpc.validation.backend=PGV").run(ctx -> {
            assertThat(ctx).hasSingleBean(ValidatingClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ValidatingServerInterceptor.class);
            assertThat(ctx).doesNotHaveBean(ProtoValidateClientInterceptor.class);
            assertThat(ctx).doesNotHaveBean(ProtoValidateServerInterceptor.class);
        });

        runner.withPropertyValues("grpc.validation.backend=pgv").run(ctx -> {
            assertThat(ctx).hasSingleBean(ValidatingClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ValidatingServerInterceptor.class);
            assertThat(ctx).doesNotHaveBean(ProtoValidateClientInterceptor.class);
            assertThat(ctx).doesNotHaveBean(ProtoValidateServerInterceptor.class);
        });
    }

    @Test
    @ClasspathReplacer(
            value = {@Action(verb = EXCLUDE, value = "build.buf:protovalidate")},
            recursiveExclude = true)
    void testWithoutProtoValidate() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GrpcValidationAutoConfiguration.class));

        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ValidatingClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ValidatingServerInterceptor.class);
            assertThat(ctx).hasBean("grpcValidatingClientInterceptor");
            assertThat(ctx).hasBean("grpcValidatingServerInterceptor");
            assertThat(ctx).doesNotHaveBean(ProtoValidateClientInterceptor.class);
            assertThat(ctx).doesNotHaveBean(ProtoValidateServerInterceptor.class);
        });
    }

    @Test
    @ClasspathReplacer(value = {@Action(verb = EXCLUDE, value = "build.buf.protoc-gen-validate:pgv-java-stub")})
    void testWithoutPgv() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GrpcValidationAutoConfiguration.class));

        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean("grpcValidatingClientInterceptor");
            assertThat(ctx).doesNotHaveBean("grpcValidatingServerInterceptor");
            assertThat(ctx).hasSingleBean(ProtoValidateClientInterceptor.class);
            assertThat(ctx).hasSingleBean(ProtoValidateServerInterceptor.class);
        });
    }
}
