package grpcstarter.server.feature.reflection;

import grpcstarter.server.GrpcServerProperties;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = GrpcServerProperties.Reflection.PREFIX, name = "enabled")
public class Reflection {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ProtoReflectionServiceV1.class)
    static class V1 {
        @Bean
        @ConditionalOnMissingBean
        public ServerReflectionGrpc.ServerReflectionImplBase grpcReflectionService() {
            return (ServerReflectionGrpc.ServerReflectionImplBase) ProtoReflectionServiceV1.newInstance();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ProtoReflectionService.class)
    static class V1Alpha {
        /**
         * For backward compatibility, many tools still use this API, such as grpcurl, postman, etc.
         */
        @Bean
        @ConditionalOnMissingBean
        public ProtoReflectionService legacyGrpcReflectionService() {
            return (ProtoReflectionService) ProtoReflectionService.newInstance();
        }
    }
}
