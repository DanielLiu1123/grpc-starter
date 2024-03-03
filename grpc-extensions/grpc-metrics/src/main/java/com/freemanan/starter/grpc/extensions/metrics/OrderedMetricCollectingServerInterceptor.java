package com.freemanan.starter.grpc.extensions.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor;
import org.springframework.core.Ordered;

/**
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
