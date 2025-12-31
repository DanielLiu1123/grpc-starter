package grpcstarter.extensions.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor;
import org.springframework.core.Ordered;

/**
 * Ordered gRPC server interceptor for Micrometer metrics.
 *
 * @author Freeman
 */
public class OrderedMetricCollectingServerInterceptor extends MetricCollectingServerInterceptor implements Ordered {

    private final int order;

    public OrderedMetricCollectingServerInterceptor(MeterRegistry registry, int order) {
        super(registry);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
