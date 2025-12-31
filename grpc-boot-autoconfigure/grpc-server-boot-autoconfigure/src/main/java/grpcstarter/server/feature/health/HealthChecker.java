package grpcstarter.server.feature.health;

/**
 * gRPC Health Checker.
 *
 * @author Freeman
 */
public interface HealthChecker {

    /**
     * Get the service name.
     *
     * @return the service name
     */
    String service();

    /**
     * Check the service status.
     *
     * @return true if the service is healthy
     */
    boolean check();
}
