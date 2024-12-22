package grpcstarter.server.feature.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link Reflection}
 */
class ReflectionTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(Reflection.class);

    @Test
    void whenEnabled_thenHasReflectionBeans() {
        runner.withPropertyValues("grpc.server.reflection.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(ServerReflectionGrpc.ServerReflectionImplBase.class);
            assertThat(ctx).hasSingleBean(ProtoReflectionService.class);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"" /*disabled by default*/, "grpc.server.reflection.enabled=false" /*explicitly disabled*/})
    void whenDisabled_thenNoReflectionBeans(String property) {
        runner.withPropertyValues(property).run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ServerReflectionGrpc.ServerReflectionImplBase.class);
            assertThat(ctx).doesNotHaveBean(ProtoReflectionService.class);
        });
    }
}
