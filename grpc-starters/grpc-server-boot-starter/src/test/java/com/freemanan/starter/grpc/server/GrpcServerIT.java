package com.freemanan.starter.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.grpc.server.feature.exceptionhandling.DefaultExceptionHandler;
import io.grpc.Server;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Freeman
 */
class GrpcServerIT {

    @Test
    void testGrpcServer() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class).run();

        assertThatCode(() -> ctx.getBean(GrpcServerProperties.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(GrpcServer.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(ProtoReflectionService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatCode(() -> ctx.getBean(DefaultExceptionHandler.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testGrpcServer_whenDisableServer() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".enabled=false")
                .run();

        assertThatCode(() -> ctx.getBean(GrpcServerProperties.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatCode(() -> ctx.getBean(GrpcServer.class)).isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testDebugEnabled() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".debug.enabled=true")
                .run();

        assertThatCode(() -> ctx.getBean(ProtoReflectionService.class)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testDefaultExceptionHandlingEnabled() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.ExceptionHandling.PREFIX + ".use-default=true")
                .run();

        assertThatCode(() -> ctx.getBean(DefaultExceptionHandler.class)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testInProcess() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + UUID.randomUUID())
                .run();

        Server server = (Server) ReflectionTestUtils.getField(ctx.getBean(GrpcServer.class), "server");

        assertThat(server).isNotNull();
        assertThat(server.getPort()).isEqualTo(-1);

        ctx.close();
    }

    @Test
    void testRandomPort() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=0")
                .run();

        Server server = (Server) ReflectionTestUtils.getField(ctx.getBean(GrpcServer.class), "server");

        assertThat(server).isNotNull();
        assertThat(server.getPort()).isNotEqualTo(9090);
        assertThat(server.getPort()).isNotEqualTo(-1);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
