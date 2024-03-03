package com.freemanan.starter.grpc.extensions.metrics;

import com.freemanan.starter.grpc.client.ConditionOnGrpcClientEnabled;
import com.freemanan.starter.grpc.client.GrpcClientProperties;
import com.freemanan.starter.grpc.server.ConditionOnGrpcServerEnabled;
import com.freemanan.starter.grpc.server.GrpcServerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingClientInterceptor;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor;
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
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = GrpcMetricsProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcMetricsProperties.class)
public class GrpcMetricsAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({MetricCollectingServerInterceptor.class, GrpcServerProperties.class})
    @ConditionalOnProperty(prefix = GrpcMetricsProperties.Server.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionOnGrpcServerEnabled
    static class Server {

        @Bean
        @ConditionalOnMissingBean(MetricCollectingServerInterceptor.class)
        public OrderedMetricCollectingServerInterceptor orderedMetricCollectingServerInterceptor(
                MeterRegistry registry, GrpcMetricsProperties properties) {
            return new OrderedMetricCollectingServerInterceptor(
                    registry, properties.getServer().getOrder());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({MetricCollectingClientInterceptor.class, GrpcClientProperties.class})
    @ConditionalOnProperty(prefix = GrpcMetricsProperties.Client.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionOnGrpcClientEnabled
    static class Client {

        @Bean
        @ConditionalOnMissingBean(MetricCollectingClientInterceptor.class)
        public OrderedMetricCollectingClientInterceptor orderedMetricCollectingClientInterceptor(
                MeterRegistry registry, GrpcMetricsProperties properties) {
            return new OrderedMetricCollectingClientInterceptor(
                    registry, properties.getClient().getOrder());
        }
    }
}
