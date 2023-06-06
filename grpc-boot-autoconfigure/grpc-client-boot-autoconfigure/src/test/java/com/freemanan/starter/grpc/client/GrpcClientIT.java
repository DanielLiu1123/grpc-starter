package com.freemanan.starter.grpc.client;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.sample.pet.v1.PetServiceGrpc;
import com.freemanan.starter.grpc.extensions.test.NetUtil;
import com.freemanan.starter.grpc.server.GrpcServerProperties;
import io.grpc.health.v1.HealthGrpc;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
class GrpcClientIT {

    @Test
    void testGrpcStubAutowired_whenNotConfigureBasePackages() {
        String name = UUID.randomUUID().toString();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .run();

        assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(PetServiceGrpc.PetServiceBlockingStub.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testGrpcStubAutowired_whenNotConfigureAuthority() {
        String name = UUID.randomUUID().toString();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=com")
                .run();

        assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(PetServiceGrpc.PetServiceBlockingStub.class))
                .isInstanceOf(BeanCreationException.class)
                .rootCause()
                .hasMessageStartingWith("Not configure authority for stub");

        ctx.close();
    }

    @Test
    void testGrpcStubAutowired_whenOK() {
        String name = UUID.randomUUID().toString();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .properties(GrpcClientProperties.InProcess.PREFIX + ".name=" + name)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=com")
                .run();

        assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(PetServiceGrpc.PetServiceBlockingStub.class))
                .doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testGrpcStubAutowired_whenDisableGrpcClient() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=0")
                .properties(GrpcClientProperties.PREFIX + ".enabled=false")
                .run();

        assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatCode(() -> ctx.getBean(PetServiceGrpc.PetServiceBlockingStub.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testGrpcStubAutowired_whenWildcardServices() {
        int port = NetUtil.getRandomPort();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=" + port)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io.grpc")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:" + port)
                .properties(GrpcClientProperties.PREFIX + ".channels[0].services[0]=grpc.**")
                .run();

        assertThatCode(() -> ctx.getBean(HealthGrpc.HealthBlockingStub.class)).doesNotThrowAnyException();

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
