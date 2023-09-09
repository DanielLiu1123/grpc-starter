package com.freemanan.starter.grpc.extensions.discovery;

import com.freemanan.starter.grpc.client.ConditionOnGrpcClientEnabled;
import com.freemanan.starter.grpc.client.GrpcClientProperties;
import com.freemanan.starter.grpc.extensions.discovery.registration.consul.Consul;
import com.freemanan.starter.grpc.extensions.discovery.registration.eureka.Eureka;
import com.freemanan.starter.grpc.server.ConditionOnGrpcServerEnabled;
import java.util.List;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = GrpcDiscoveryProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcDiscoveryProperties.class)
public class GrpcDiscoveryAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = GrpcDiscoveryProperties.Server.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionOnGrpcServerEnabled
    @ImportAutoConfiguration({Consul.class, Eureka.class})
    static class Server {}

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = GrpcDiscoveryProperties.Client.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionOnGrpcClientEnabled
    @ConditionalOnClass(LoadBalancerClientFactory.class)
    static class Client {
        @Bean
        @ConditionalOnMissingBean
        public LoadBalancerClientInterceptor grpcLoadBalancerClientInterceptor(
                LoadBalancerClientFactory loadBalancerClientFactory, List<GrpcChannelTransformer> channelTransformers) {
            return new LoadBalancerClientInterceptor(loadBalancerClientFactory, channelTransformers);
        }

        @Bean
        @ConditionalOnMissingBean
        public ConfigurationGrpcChannelTransformer configurationGrpcChannelTransformer(
                GrpcClientProperties grpcClientProperties) {
            return new ConfigurationGrpcChannelTransformer(grpcClientProperties);
        }
    }
}
