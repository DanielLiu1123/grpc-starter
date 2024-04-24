package grpcstarter.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.grpc.protobuf.services.ProtoReflectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
class GrpcServerIT {

    @Test
    void testGrpcServer() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.port=0")
                .run();

        assertThatCode(() -> ctx.getBean(GrpcServerProperties.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(DefaultGrpcServer.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(GrpcRequestContextServerInterceptor.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(ProtoReflectionService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testGrpcServer_whenDisableServer() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".enabled=false")
                .run();

        assertThatCode(() -> ctx.getBean(GrpcServerProperties.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatCode(() -> ctx.getBean(DefaultGrpcServer.class)).isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testReflectionEnabled() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.port=0")
                .properties(GrpcServerProperties.PREFIX + ".reflection.enabled=true")
                .run();

        assertThatCode(() -> ctx.getBean(ProtoReflectionService.class)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testRandomPort() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=0")
                .run();

        int port = ctx.getBean(GrpcServer.class).getPort();

        assertThat(port).isNotEqualTo(9090).isNotEqualTo(-1);

        ctx.close();
    }

    @Test
    void testEmptyServerEnabled() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.port=0")
                .run();

        GrpcServer server = ctx.getBean(GrpcServer.class);

        assertThat(server).isInstanceOf(DefaultGrpcServer.class);

        ctx.close();
    }

    @Test
    void testEmptyServerDisabled() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".enable-empty-server=false")
                .run();

        GrpcServer server = ctx.getBean(GrpcServer.class);

        assertThat(server).isInstanceOf(DummyGrpcServer.class);
        assertThat(server.getPort()).isEqualTo(-1);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
