package com.freemanan.starter.grpc.server;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.grpc.channelz.v1.ChannelzGrpc;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
class ChannelzIT {

    @Test
    void testDisabledByDefault() {
        String name = UUID.randomUUID().toString();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .run();

        assertThatCode(() -> ctx.getBean(ChannelzGrpc.ChannelzImplBase.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testEnableChannelz() {
        String name = UUID.randomUUID().toString();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .properties(GrpcServerProperties.Channelz.PREFIX + ".enabled=true")
                .run();

        assertThatCode(() -> ctx.getBean(ChannelzGrpc.ChannelzImplBase.class)).doesNotThrowAnyException();

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
