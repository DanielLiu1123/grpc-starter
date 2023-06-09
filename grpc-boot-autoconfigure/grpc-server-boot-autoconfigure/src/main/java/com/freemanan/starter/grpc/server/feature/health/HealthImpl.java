package com.freemanan.starter.grpc.server.feature.health;

import static io.grpc.health.v1.HealthCheckResponse.ServingStatus;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
public class HealthImpl extends HealthGrpc.HealthImplBase {
    private static final Logger log = LoggerFactory.getLogger(HealthImpl.class);

    /**
     * service name to {@link HealthChecker}.
     */
    private final Map<String, HealthChecker> serviceToChecker;

    public HealthImpl(ObjectProvider<HealthChecker> healthCheckers) {
        this.serviceToChecker = healthCheckers
                .orderedStream()
                .collect(Collectors.toMap(
                        HealthChecker::service, Function.identity(), (oldV, newV) -> oldV, LinkedHashMap::new));
    }

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> so) {
        String service = request.getService();

        if (StringUtils.hasText(service)) {
            HealthChecker checker = serviceToChecker.get(service);
            if (checker == null) {
                // see io.grpc.protobuf.services.HealthServiceImpl#check
                so.onError(new StatusException(Status.NOT_FOUND.withDescription("unknown service " + service)));
            } else {
                boolean healthy = checker.check();
                ServingStatus status = healthy ? ServingStatus.SERVING : ServingStatus.NOT_SERVING;
                so.onNext(HealthCheckResponse.newBuilder().setStatus(status).build());
                so.onCompleted();
            }
        } else {
            boolean healthy = true;
            for (Map.Entry<String, HealthChecker> en : serviceToChecker.entrySet()) {
                HealthChecker checker = en.getValue();
                if (!checker.check()) {
                    healthy = false;
                    log.warn("Health check failed: {}", checker.getClass().getSimpleName());
                    break;
                }
            }
            ServingStatus status = healthy ? ServingStatus.SERVING : ServingStatus.NOT_SERVING;
            so.onNext(HealthCheckResponse.newBuilder().setStatus(status).build());
            so.onCompleted();
        }
    }

    @Override
    public void watch(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        // TODO(Freeman): need to implement it?
        super.watch(request, responseObserver);
    }
}
