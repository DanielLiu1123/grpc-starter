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
@ConditionalOnClass(ServerReflectionGrpc.ServerReflectionImplBase.class)
@ConditionalOnProperty(prefix = GrpcServerProperties.Reflection.PREFIX, name = "enabled")
public class Reflection {

    @Bean
    @ConditionalOnMissingBean
    public ServerReflectionGrpc.ServerReflectionImplBase grpcReflectionService() {
        return (ServerReflectionGrpc.ServerReflectionImplBase) ProtoReflectionServiceV1.newInstance();
    }

    /**
     * For backward compatibility.
     */
    @Bean
    @ConditionalOnMissingBean
    public ProtoReflectionService legacyGrpcReflectionService() {
        return (ProtoReflectionService) ProtoReflectionService.newInstance();
    }
}
