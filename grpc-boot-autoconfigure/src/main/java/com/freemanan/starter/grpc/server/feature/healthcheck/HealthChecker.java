package com.freemanan.starter.grpc.server.feature.healthcheck;

/**
 * @author Freeman
 */
public interface HealthChecker {

    /**
     * @return true if the service is healthy
     */
    boolean check();
}
