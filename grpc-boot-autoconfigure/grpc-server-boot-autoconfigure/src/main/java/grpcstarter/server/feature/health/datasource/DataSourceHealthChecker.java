package grpcstarter.server.feature.health.datasource;

import grpcstarter.server.GrpcServerProperties;
import grpcstarter.server.feature.health.HealthChecker;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

/**
 * @author Freeman
 */
public class DataSourceHealthChecker implements HealthChecker, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(DataSourceHealthChecker.class);

    private final ApplicationContext ctx;
    private final List<DataSource> dataSources = new ArrayList<>();
    private final GrpcServerProperties.Health.DataSource config;

    public DataSourceHealthChecker(ApplicationContext ctx, GrpcServerProperties.Health.DataSource config) {
        this.ctx = ctx;
        this.config = config;
    }

    @Override
    public String service() {
        return config.getService();
    }

    @Override
    public boolean check() {
        for (DataSource dataSource : dataSources) {
            if (!isDataSourceHealthy(dataSource)) {
                return false;
            }
        }
        return true;
    }

    private boolean isDataSourceHealthy(DataSource dataSource) {
        try (var conn = dataSource.getConnection()) {
            int timeout = config.getTimeout() != null ? config.getTimeout() : 0;
            return conn.isValid(timeout);
        } catch (SQLException e) {
            log.warn("DataSource health check failed for DataSource: {}", dataSource, e);
            return false;
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        // Do NOT inject DataSource here, we don't want to effect the order of auto-configurations
        List<DataSource> sources =
                ctx.getBeanProvider(DataSource.class).orderedStream().toList();
        this.dataSources.addAll(sources);
    }
}
