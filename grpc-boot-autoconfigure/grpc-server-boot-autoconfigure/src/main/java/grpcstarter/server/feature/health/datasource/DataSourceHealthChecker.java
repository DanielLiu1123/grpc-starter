package grpcstarter.server.feature.health.datasource;

import grpcstarter.server.GrpcServerProperties;
import grpcstarter.server.feature.health.HealthChecker;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * @author Freeman
 */
public class DataSourceHealthChecker implements HealthChecker, BeanFactoryAware, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(DataSourceHealthChecker.class);

    private BeanFactory beanFactory;
    private final List<DataSource> dataSources = new ArrayList<>();
    private final GrpcServerProperties.Health.DataSource config;

    public DataSourceHealthChecker(GrpcServerProperties.Health.DataSource config) {
        this.config = config;
    }

    @Override
    public String service() {
        return config.getService();
    }

    @Override
    public boolean check() {
        for (DataSource dataSource : dataSources) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement statement = conn.prepareStatement(config.getValidationQuery())) {
                if (config.getTimeout() != null) {
                    statement.setQueryTimeout(config.getTimeout());
                }
                statement.execute();
            } catch (Exception e) {
                log.warn("DataSource health check failed!", e);
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
        // Do NOT inject DataSource here, we don't want to effect the order of auto-configurations
        List<DataSource> sources =
                beanFactory.getBeanProvider(DataSource.class).orderedStream().collect(Collectors.toList());
        this.dataSources.addAll(sources);
    }
}
