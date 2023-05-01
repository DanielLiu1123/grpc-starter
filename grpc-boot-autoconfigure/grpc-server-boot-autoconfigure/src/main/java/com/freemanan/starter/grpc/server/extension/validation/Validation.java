package com.freemanan.starter.grpc.server.extension.validation;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import io.envoyproxy.pgv.ReflectiveValidatorIndex;
import io.envoyproxy.pgv.grpc.ValidatingServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = GrpcServerProperties.Validation.PREFIX, name = "enabled", matchIfMissing = true)
public class Validation {
    public static final int ORDER = 0;

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ValidatingServerInterceptor.class)
    static class Pgv {

        @Bean
        @Order(ORDER)
        @ConditionalOnMissingBean
        public ValidatingServerInterceptor grpcValidatingServerInterceptor() {
            // TODO(Freeman): not return pgv message directly? It may be bigger that the max metadata size.
            return new ValidatingServerInterceptor(new ReflectiveValidatorIndex());
        }
    }
}
