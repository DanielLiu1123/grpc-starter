package grpcstarter.server.feature.channelz;

import grpcstarter.server.GrpcServerProperties;
import io.grpc.channelz.v1.ChannelzGrpc;
import io.grpc.protobuf.services.ChannelzService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ChannelzGrpc.ChannelzImplBase.class)
@ConditionalOnProperty(prefix = GrpcServerProperties.Channelz.PREFIX, name = "enabled")
public class Channelz {

    @Bean
    @ConditionalOnMissingBean
    public ChannelzGrpc.ChannelzImplBase grpcChannelzService(GrpcServerProperties properties) {
        return ChannelzService.newInstance(properties.getChannelz().getMaxPageSize());
    }
}
