package com.freemanan.starter.grpc.extensions.validation;

import io.envoyproxy.pgv.ValidatorIndex;
import io.envoyproxy.pgv.grpc.ValidatingServerInterceptor;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
class OrderedValidatingServerInterceptor extends ValidatingServerInterceptor implements Ordered {

    private final int order;

    public OrderedValidatingServerInterceptor(ValidatorIndex index, int order) {
        super(index);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
