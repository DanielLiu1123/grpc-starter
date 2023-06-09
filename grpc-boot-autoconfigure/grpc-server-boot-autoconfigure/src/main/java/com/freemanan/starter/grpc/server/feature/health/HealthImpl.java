package com.freemanan.starter.grpc.server.feature.health;

import static io.grpc.health.v1.HealthCheckResponse.ServingStatus;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

/**
 * TODO(Freeman): need refactor, see HealthServiceImpl
 *
 * @author Freeman
 */
public class HealthImpl extends HealthGrpc.HealthImplBase {
    private static final Logger log = LoggerFactory.getLogger(HealthImpl.class);

    private final List<HealthChecker> healthCheckers;

    public HealthImpl(ObjectProvider<HealthChecker> healthCheckers) {
        this.healthCheckers = healthCheckers.orderedStream().collect(Collectors.toList());
    }

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> so) {
        List<HealthChecker> checkers = getCheckers(request.getService());

        boolean healthy = true;
        for (HealthChecker healthChecker : checkers) {
            if (!healthChecker.check()) {
                healthy = false;
                log.warn("Health check failed: {}", healthChecker.getClass().getSimpleName());
                break;
            }
        }
        ServingStatus status = healthy ? ServingStatus.SERVING : ServingStatus.NOT_SERVING;
        so.onNext(HealthCheckResponse.newBuilder().setStatus(status).build());
        so.onCompleted();
    }

    @Override
    public void watch(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        // TODO(Freeman): need to implement it?
        super.watch(request, responseObserver);
    }

    protected List<HealthChecker> getCheckers(String service) {
        return StringUtils.hasText(service)
                ? healthCheckers.stream()
                        .filter(healthChecker -> service.equalsIgnoreCase(healthChecker.service()))
                        .collect(Collectors.toList())
                : healthCheckers;
    }
}
