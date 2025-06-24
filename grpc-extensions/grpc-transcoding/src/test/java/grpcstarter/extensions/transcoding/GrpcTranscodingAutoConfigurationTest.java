package grpcstarter.extensions.transcoding;

import static org.assertj.core.api.Assertions.assertThat;

import grpcstarter.server.GrpcServerAutoConfiguration;
import java.util.Objects;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * {@link GrpcTranscodingAutoConfiguration} tester.
 */
class GrpcTranscodingAutoConfigurationTest {

    final WebApplicationContextRunner servletRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GrpcTranscodingAutoConfiguration.class,
                    GrpcServerAutoConfiguration.class,
                    SslAutoConfiguration.class));

    final ReactiveWebApplicationContextRunner reactiveRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GrpcTranscodingAutoConfiguration.class,
                    GrpcServerAutoConfiguration.class,
                    SslAutoConfiguration.class));

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testDefaultBehavior(String webType) {
        if (Objects.equals(webType, WebApplicationType.SERVLET.name())) {
            servletRunner.withPropertyValues("grpc.server.port=0").run(context -> {
                assertThat(context).hasSingleBean(GrpcTranscodingAutoConfiguration.class);
                assertThat(context).hasSingleBean(GrpcTranscodingProperties.class);
                assertThat(context).hasSingleBean(HeaderConverter.class);
                assertThat(context).hasSingleBean(ServletTranscoder.class);
                assertThat(context).hasSingleBean(TranscodingExceptionResolver.class);
                assertThat(context).doesNotHaveBean(ReactiveTranscoder.class);
                assertThat(context).doesNotHaveBean(ReactiveTranscodingExceptionResolver.class);
            });
        }
        if (Objects.equals(webType, WebApplicationType.REACTIVE.name())) {
            reactiveRunner.withPropertyValues("grpc.server.port=0").run(context -> {
                assertThat(context).hasSingleBean(GrpcTranscodingAutoConfiguration.class);
                assertThat(context).hasSingleBean(GrpcTranscodingProperties.class);
                assertThat(context).hasSingleBean(HeaderConverter.class);
                assertThat(context).hasSingleBean(ReactiveTranscoder.class);
                assertThat(context).hasSingleBean(ReactiveTranscodingExceptionResolver.class);
                assertThat(context).doesNotHaveBean(ServletTranscoder.class);
                assertThat(context).doesNotHaveBean(TranscodingExceptionResolver.class);
            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testGrpcServerDisabled(String webType) {
        if (Objects.equals(webType, WebApplicationType.SERVLET.name())) {
            servletRunner.withPropertyValues("grpc.server.enabled=false").run(context -> assertThat(context)
                    .doesNotHaveBean(GrpcTranscodingAutoConfiguration.class));
        }
        if (Objects.equals(webType, WebApplicationType.REACTIVE.name())) {
            reactiveRunner.withPropertyValues("grpc.server.enabled=false").run(context -> assertThat(context)
                    .doesNotHaveBean(GrpcTranscodingAutoConfiguration.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERVLET", "REACTIVE"})
    void testGrpcTranscodingDisabled(String webType) {
        if (Objects.equals(webType, WebApplicationType.SERVLET.name())) {
            servletRunner
                    .withPropertyValues("grpc.transcoding.enabled=false")
                    .withPropertyValues("grpc.server.port=0")
                    .run(context -> assertThat(context).doesNotHaveBean(GrpcTranscodingAutoConfiguration.class));
        }
        if (Objects.equals(webType, WebApplicationType.REACTIVE.name())) {
            reactiveRunner
                    .withPropertyValues("grpc.transcoding.enabled=false")
                    .withPropertyValues("grpc.server.port=0")
                    .run(context -> assertThat(context).doesNotHaveBean(GrpcTranscodingAutoConfiguration.class));
        }
    }
}
