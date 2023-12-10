package com.freemanan.starter.grpc.client;

import static com.freemanan.starter.grpc.client.Checker.checkUnusedConfig;

import com.freemanan.starter.grpc.server.GrpcServerShutdownEvent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionOnGrpcClientEnabled
@EnableConfigurationProperties(GrpcClientProperties.class)
public class GrpcClientAutoConfiguration implements DisposableBean {

    @Bean
    static GrpcStubBeanDefinitionRegistry grpcStubBeanDefinitionRegistry() {
        return new GrpcStubBeanDefinitionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcClientOptionsClientInterceptor grpcClientOptionsClientInterceptor() {
        return new GrpcClientOptionsClientInterceptor();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = GrpcClientProperties.PREFIX,
            name = "warn-unused-config-enabled",
            matchIfMissing = true)
    public CommandLineRunner grpcClientUnusedConfigChecker(GrpcClientProperties properties) {
        return args -> checkUnusedConfig(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RefreshScopeRefreshedEvent.class)
    static class SpringCloudConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = GrpcClientProperties.Refresh.PREFIX, name = "enabled", havingValue = "true")
        public RefreshScopeRefreshedEventListener grpcClientRefreshScopeRefreshedEventListener() {
            return new RefreshScopeRefreshedEventListener();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(GrpcServerShutdownEvent.class)
    static class ChannelCloserConfiguration {

        @Bean
        public ShutdownEventBasedChannelCloser shutdownEventBasedChannelCloser() {
            return new ShutdownEventBasedChannelCloser();
        }
    }

    @Override
    public void destroy() {
        Cache.clear();
    }
}
