package com.freemanan.starter.grpc.extensions.tracing;

import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
public class OrderedObservationGrpcServerInterceptor extends ObservationGrpcServerInterceptor implements Ordered {

    private final int order;

    public OrderedObservationGrpcServerInterceptor(ObservationRegistry observationRegistry, int order) {
        super(observationRegistry);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
