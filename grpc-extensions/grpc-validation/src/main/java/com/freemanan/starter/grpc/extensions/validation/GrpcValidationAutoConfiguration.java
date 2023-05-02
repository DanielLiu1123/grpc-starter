package com.freemanan.starter.grpc.extensions.validation;

import io.envoyproxy.pgv.ReflectiveValidatorIndex;
import io.envoyproxy.pgv.grpc.ValidatingClientInterceptor;
import io.envoyproxy.pgv.grpc.ValidatingServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = GrpcValidationProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcValidationProperties.class)
public class GrpcValidationAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ValidatingClientInterceptor.class)
    @ConditionalOnProperty(prefix = GrpcValidationProperties.Client.PREFIX, name = "enabled", matchIfMissing = true)
    static class Client {

        @Bean
        @ConditionalOnMissingBean
        public ValidatingClientInterceptor grpcValidatingClientInterceptor(GrpcValidationProperties properties) {
            return new OrderedValidatingClientInterceptor(
                    new ReflectiveValidatorIndex(), properties.getClient().getOrder());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ValidatingServerInterceptor.class)
    @ConditionalOnProperty(prefix = GrpcValidationProperties.Server.PREFIX, name = "enabled", matchIfMissing = true)
    static class Server {

        @Bean
        @ConditionalOnMissingBean
        public ValidatingServerInterceptor grpcValidatingServerInterceptor(GrpcValidationProperties properties) {
            return new OrderedValidatingServerInterceptor(
                    new ReflectiveValidatorIndex(), properties.getServer().getOrder());
        }
    }
}
