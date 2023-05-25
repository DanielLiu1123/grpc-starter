package com.freemanan.starter.grpc.extensions.validation;

import com.freemanan.starter.grpc.client.ConditionOnGrpcClientEnabled;
import com.freemanan.starter.grpc.client.GrpcClientProperties;
import com.freemanan.starter.grpc.server.ConditionOnGrpcServerEnabled;
import com.freemanan.starter.grpc.server.GrpcServerProperties;
import io.envoyproxy.pgv.ReflectiveValidatorIndex;
import io.envoyproxy.pgv.ValidatorIndex;
import io.envoyproxy.pgv.grpc.ValidatingClientInterceptor;
import io.envoyproxy.pgv.grpc.ValidatingServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = GrpcValidationProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcValidationProperties.class)
public class GrpcValidationAutoConfiguration {

    /**
     * Validation implementation based on PGV.
     *
     * @see <a href="https://github.com/bufbuild/protoc-gen-validate">pgv</a>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ValidatorIndex.class)
    static class Pgv {

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass({ValidatingClientInterceptor.class, GrpcClientProperties.class})
        @ConditionOnGrpcClientEnabled
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
        @ConditionalOnClass({ValidatingServerInterceptor.class, GrpcServerProperties.class})
        @ConditionOnGrpcServerEnabled
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
}
