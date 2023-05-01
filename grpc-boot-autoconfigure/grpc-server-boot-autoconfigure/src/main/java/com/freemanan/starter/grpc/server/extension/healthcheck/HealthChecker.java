package com.freemanan.starter.grpc.server.extension.healthcheck;

/**
 * @author Freeman
 */
public interface HealthChecker {

    /**
     * Check the service status.
     *
     * @return true if the service is healthy
     */
    boolean check();
}
