package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractFutureStub;
import io.grpc.stub.AbstractStub;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * {@link GrpcClientBeanDefinitionHandlerIT}
 */
class GrpcClientBeanDefinitionHandlerIT {

    /**
     * {@link GrpcClientBeanDefinitionHandler.Default}
     */
    @Test
    void testDefault() {
        var inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg1.class)
                .properties("grpc.server.in-process.name=" + inProcessName)
                .properties("grpc.client.in-process.name=" + inProcessName)
                .run()) {

            var actual = getStubSuperClasses(ctx);

            var expected = Set.of(AbstractBlockingStub.class, AbstractFutureStub.class, AbstractAsyncStub.class);

            assertThat(actual).isEqualTo(expected);
        }
    }

    /**
     * {@link GrpcClientBeanDefinitionHandler.Blocking}
     */
    @Test
    void testBlocking() {
        var inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg1.class)
                .properties("grpc.server.in-process.name=" + inProcessName)
                .properties("grpc.client.in-process.name=" + inProcessName)
                .properties("grpc.client.bean-definition-handler="
                        + GrpcClientBeanDefinitionHandler.Blocking.class.getName())
                .run()) {

            var actual = getStubSuperClasses(ctx);

            var expected = Set.of(AbstractBlockingStub.class);

            assertThat(actual).isEqualTo(expected);
        }
    }

    /**
     * {@link GrpcClientBeanDefinitionHandler.Future}
     */
    @Test
    void testFuture() {
        var inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg1.class)
                .properties("grpc.server.in-process.name=" + inProcessName)
                .properties("grpc.client.in-process.name=" + inProcessName)
                .properties(
                        "grpc.client.bean-definition-handler=" + GrpcClientBeanDefinitionHandler.Future.class.getName())
                .run()) {

            var actual = getStubSuperClasses(ctx);

            var expected = Set.of(AbstractFutureStub.class);

            assertThat(actual).isEqualTo(expected);
        }
    }

    /**
     * {@link GrpcClientBeanDefinitionHandler.Async}
     */
    @Test
    void testAsync() {
        var inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg1.class)
                .properties("grpc.server.in-process.name=" + inProcessName)
                .properties("grpc.client.in-process.name=" + inProcessName)
                .properties(
                        "grpc.client.bean-definition-handler=" + GrpcClientBeanDefinitionHandler.Async.class.getName())
                .run()) {

            var actual = getStubSuperClasses(ctx);

            var expected = Set.of(AbstractAsyncStub.class);

            assertThat(actual).isEqualTo(expected);
        }
    }

    private static Set<Class<?>> getStubSuperClasses(ConfigurableApplicationContext ctx) {
        return ctx.getBeansOfType(AbstractStub.class).values().stream()
                .map(e -> e.getClass().getSuperclass())
                .collect(Collectors.toSet());
    }

    /**
     * {@link NoneBeanDefinitionHandler}
     */
    @Test
    void testNoneBeanDefinitionHandler() {
        var inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg1.class)
                .properties("grpc.server.in-process.name=" + inProcessName)
                .properties("grpc.client.in-process.name=" + inProcessName)
                .properties("grpc.client.bean-definition-handler=" + NoneBeanDefinitionHandler.class.getName())
                .run()) {
            var stubMap = ctx.getBeansOfType(AbstractStub.class);

            assertThat(stubMap).isEmpty();
        }
    }

    @Test
    void testEnableGrpcClientsHasHigherPriorityThanConfig() {
        var inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg2.class)
                .properties("grpc.server.in-process.name=" + inProcessName)
                .properties("grpc.client.in-process.name=" + inProcessName)
                .properties("grpc.client.bean-definition-handler=" + NoneBeanDefinitionHandler.class.getName())
                .run()) {
            var stubMap = ctx.getBeansOfType(AbstractStub.class);

            assertThat(stubMap).isNotEmpty();
            assertThat(stubMap).allSatisfy((name, stub) -> assertThat(stub).isInstanceOf(AbstractBlockingStub.class));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableGrpcClients(basePackages = "io.grpc")
    static class Cfg1 {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableGrpcClients(basePackages = "io.grpc", beanDefinitionHandler = GrpcClientBeanDefinitionHandler.Blocking.class)
    static class Cfg2 {}

    static class NoneBeanDefinitionHandler implements GrpcClientBeanDefinitionHandler {
        @Nullable
        @Override
        public BeanDefinition handle(BeanDefinition beanDefinition, Class<?> clazz) {
            return null;
        }
    }
}
