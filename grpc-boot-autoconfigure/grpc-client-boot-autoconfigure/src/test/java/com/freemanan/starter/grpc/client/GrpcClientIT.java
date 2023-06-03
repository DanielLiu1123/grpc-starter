package com.freemanan.starter.grpc.client;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.sample.pet.v1.PetServiceGrpc;
import com.freemanan.starter.grpc.server.GrpcServerProperties;
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
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=0")
                .run();

        assertThatCode(() -> ctx.getBean(GrpcClientProperties.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(PetServiceGrpc.PetServiceBlockingStub.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testGrpcStubAutowired_whenNotConfigureAuthority() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=0")
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
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.PREFIX + ".port=0")
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=com")
                .properties(GrpcClientProperties.PREFIX + ".authority=localhost:8080")
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

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
