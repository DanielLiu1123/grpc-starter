package com.freemanan.starter.grpc.server.feature.validation;

import com.freemanan.starter.grpc.GrpcProperties;
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
@ConditionalOnProperty(prefix = GrpcProperties.Server.Validation.PREFIX, name = "enabled", matchIfMissing = true)
public class Validation {
    public static final int ORDER = 0;

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ValidatingServerInterceptor.class)
    static class Pgv {

        @Bean
        @Order(ORDER)
        @ConditionalOnMissingBean
        public ValidatingServerInterceptor grpcValidatingServerInterceptor() {
            return new ValidatingServerInterceptor(new ReflectiveValidatorIndex());
        }
    }
}
