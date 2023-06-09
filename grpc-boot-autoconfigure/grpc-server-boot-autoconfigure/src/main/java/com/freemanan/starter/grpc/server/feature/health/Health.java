package com.freemanan.starter.grpc.server.feature.health;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import com.freemanan.starter.grpc.server.feature.health.datasource.DataSourceHealthChecker;
import com.freemanan.starter.grpc.server.feature.health.redis.RedisHealthChecker;
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
@ConditionalOnProperty(prefix = GrpcServerProperties.Health.PREFIX, name = "enabled", matchIfMissing = true)
public class Health {

    @Bean
    @ConditionalOnMissingBean
    public HealthGrpc.HealthImplBase grpcHealthImpl(ObjectProvider<HealthChecker> healthCheckers) {
        return new HealthImpl(healthCheckers);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = GrpcServerProperties.Health.DataSource.PREFIX,
            name = "enabled",
            matchIfMissing = true)
    @ConditionalOnClass(JdbcTemplate.class)
    static class DataSource {

        @Bean
        @ConditionalOnMissingBean
        public DataSourceHealthChecker grpcDataSourceHealthChecker(GrpcServerProperties properties) {
            return new DataSourceHealthChecker(properties.getHealth().getDatasource());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = GrpcServerProperties.Health.Redis.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionalOnClass(RedisConnectionFactory.class)
    static class Redis {

        @Bean
        @ConditionalOnMissingBean
        public RedisHealthChecker grpcRedisHealthChecker() {
            return new RedisHealthChecker();
        }
    }
}
