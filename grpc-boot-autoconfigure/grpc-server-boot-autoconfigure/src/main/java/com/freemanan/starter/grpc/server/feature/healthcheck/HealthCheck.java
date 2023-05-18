package com.freemanan.starter.grpc.server.feature.healthcheck;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import com.freemanan.starter.grpc.server.feature.healthcheck.datasource.DataSourceHealthChecker;
import com.freemanan.starter.grpc.server.feature.healthcheck.redis.RedisHealthChecker;
import io.grpc.health.v1.HealthGrpc;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HealthGrpc.HealthImplBase.class)
@ConditionalOnProperty(prefix = GrpcServerProperties.HealthCheck.PREFIX, name = "enabled", matchIfMissing = true)
public class HealthCheck {

    @Bean
    @ConditionalOnMissingBean
    public HealthCheckService grpcHealthCheckService(ObjectProvider<HealthChecker> healthCheckers) {
        return new HealthCheckService(healthCheckers);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = GrpcServerProperties.HealthCheck.DataSource.PREFIX,
            name = "enabled",
            matchIfMissing = true)
    @ConditionalOnClass(JdbcTemplate.class)
    static class DataSource {

        @Bean
        @ConditionalOnMissingBean
        public DataSourceHealthChecker grpcDataSourceHealthChecker(GrpcServerProperties properties) {
            return new DataSourceHealthChecker(properties.getHealthCheck().getDatasource());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = GrpcServerProperties.HealthCheck.Redis.PREFIX,
            name = "enabled",
            matchIfMissing = true)
    @ConditionalOnClass(RedisConnectionFactory.class)
    static class Redis {

        @Bean
        @ConditionalOnMissingBean
        public RedisHealthChecker grpcRedisHealthChecker() {
            return new RedisHealthChecker();
        }
    }
}
