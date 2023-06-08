package com.freemanan.starter.grpc.server.feature.healthcheck;

import static io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import static io.grpc.health.v1.HealthCheckResponse.newBuilder;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * TODO(Freeman): need refactor, see HealthServiceImpl
 *
 * @author Freeman
 */
public class HealthCheckService extends HealthGrpc.HealthImplBase {
    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final List<HealthChecker> healthCheckers;

    public HealthCheckService(ObjectProvider<HealthChecker> healthCheckers) {
        this.healthCheckers = healthCheckers.orderedStream().collect(Collectors.toList());
    }

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> so) {
        boolean healthy = true;
        for (HealthChecker healthChecker : healthCheckers) {
            if (!healthChecker.check()) {
                healthy = false;
                log.warn("Health check failed: {}", healthChecker.getClass().getSimpleName());
                break;
            }
        }
        ServingStatus status = healthy ? ServingStatus.SERVING : ServingStatus.NOT_SERVING;
        so.onNext(newBuilder().setStatus(status).build());
        so.onCompleted();
    }

    @Override
    public void watch(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        // TODO(Freeman): need to implement it?
        super.watch(request, responseObserver);
    }
}
