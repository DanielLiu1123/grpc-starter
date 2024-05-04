package grpcstarter.extensions.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingClientInterceptor;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public class OrderedMetricCollectingClientInterceptor extends MetricCollectingClientInterceptor implements Ordered {

    private final int order;

    public OrderedMetricCollectingClientInterceptor(MeterRegistry registry, int order) {
        super(registry);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
