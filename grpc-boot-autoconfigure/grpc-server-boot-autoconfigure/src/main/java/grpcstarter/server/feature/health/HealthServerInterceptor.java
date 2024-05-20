package grpcstarter.server.feature.health;

import static io.grpc.protobuf.services.HealthStatusManager.SERVICE_NAME_ALL_SERVICES;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Freeman
 */
public class HealthServerInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(HealthServerInterceptor.class);

    private static final String HEALTH_REQUEST_PREFIX = HealthGrpc.SERVICE_NAME + "/";

    private final HealthStatusManager healthManager;
    private final Map<String, HealthChecker> serviceToChecker;

    public HealthServerInterceptor(HealthStatusManager healthManager, ObjectProvider<HealthChecker> checkers) {
        this.healthManager = healthManager;
        this.serviceToChecker = checkers.orderedStream()
                .collect(Collectors.toMap(
                        HealthChecker::service,
                        Function.identity(),
                        (o, n) -> {
                            log.warn(
                                    "Duplicate service name for health checker: {}, {}, use {}",
                                    o.getClass().getSimpleName(),
                                    n.getClass().getSimpleName(),
                                    o.getClass().getSimpleName());
                            return o;
                        },
                        LinkedHashMap::new));
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return isHealthCheckRequest(call.getMethodDescriptor().getFullMethodName())
                ? new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                        next.startCall(call, headers)) {
                    @Override
                    public void onMessage(ReqT message) {
                        if (message instanceof HealthCheckRequest req) {
                            changeServiceHealthStatus(req);
                        }
                        super.onMessage(message);
                    }
                }
                : next.startCall(call, headers);
    }

    protected void changeServiceHealthStatus(HealthCheckRequest message) {
        boolean allHealthy = true;
        for (Map.Entry<String, HealthChecker> en : serviceToChecker.entrySet()) {
            String service = en.getKey();
            if (Objects.equals(message.getService(), SERVICE_NAME_ALL_SERVICES)
                    || Objects.equals(message.getService(), service)) {
                HealthChecker checker = en.getValue();
                if (checker.check()) {
                    healthManager.setStatus(service, HealthCheckResponse.ServingStatus.SERVING);
                } else {
                    allHealthy = false;
                    healthManager.setStatus(service, HealthCheckResponse.ServingStatus.NOT_SERVING);
                }
            }
        }
        HealthCheckResponse.ServingStatus status =
                allHealthy ? HealthCheckResponse.ServingStatus.SERVING : HealthCheckResponse.ServingStatus.NOT_SERVING;
        healthManager.setStatus(SERVICE_NAME_ALL_SERVICES, status);
    }

    protected static boolean isHealthCheckRequest(String fullMethodName) {
        return fullMethodName != null && fullMethodName.startsWith(HEALTH_REQUEST_PREFIX);
    }
}
