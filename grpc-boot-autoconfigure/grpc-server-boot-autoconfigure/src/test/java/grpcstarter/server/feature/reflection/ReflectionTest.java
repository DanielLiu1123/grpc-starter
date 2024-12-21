package grpcstarter.server.feature.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import org.junit.jupiter.api.Test;
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

    @Test
    void whenDisabled_thenNoReflectionBeans() {
        // disabled by default
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ServerReflectionGrpc.ServerReflectionImplBase.class);
            assertThat(ctx).doesNotHaveBean(ProtoReflectionService.class);
        });

        // explicitly disabled
        runner.withPropertyValues("grpc.server.reflection.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ServerReflectionGrpc.ServerReflectionImplBase.class);
            assertThat(ctx).doesNotHaveBean(ProtoReflectionService.class);
        });
    }
}
