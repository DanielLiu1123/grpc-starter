package grpcstarter.extensions.tracing;

import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public class OrderedObservationGrpcClientInterceptor extends ObservationGrpcClientInterceptor implements Ordered {

    private final int order;

    public OrderedObservationGrpcClientInterceptor(ObservationRegistry observationRegistry, int order) {
        super(observationRegistry);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
