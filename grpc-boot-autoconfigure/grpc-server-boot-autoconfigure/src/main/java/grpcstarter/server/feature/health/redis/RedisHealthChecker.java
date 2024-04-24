package grpcstarter.server.feature.health.redis;

import grpcstarter.server.GrpcServerProperties;
import grpcstarter.server.feature.health.HealthChecker;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author Freeman
 */
public class RedisHealthChecker implements HealthChecker, BeanFactoryAware, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(RedisHealthChecker.class);

    private BeanFactory beanFactory;
    private final List<RedisConnectionFactory> connectionFactories = new ArrayList<>();
    private final GrpcServerProperties.Health.Redis config;

    public RedisHealthChecker(GrpcServerProperties.Health.Redis config) {
        this.config = config;
    }

    @Override
    public String service() {
        return config.getService();
    }

    @Override
    public boolean check() {
        for (RedisConnectionFactory factory : connectionFactories) {
            try (RedisConnection conn = factory.getConnection()) {
                conn.ping();
            } catch (Exception e) {
                log.warn("Redis health check failed!", e);
                return false;
            }
        }
        return true;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // Do NOT inject RedisConnectionFactory here, we don't want to effect the order of auto-configurations
        List<RedisConnectionFactory> factories = beanFactory
                .getBeanProvider(RedisConnectionFactory.class)
                .orderedStream()
                .collect(Collectors.toList());
        this.connectionFactories.addAll(factories);
    }
}
