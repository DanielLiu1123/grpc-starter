package com.freemanan.starter.grpc.extensions.validation;

import io.envoyproxy.pgv.ValidatorIndex;
import io.envoyproxy.pgv.grpc.ValidatingClientInterceptor;
import org.springframework.core.Ordered;

/**
 * @author Freeman
 */
class OrderedValidatingClientInterceptor extends ValidatingClientInterceptor implements Ordered {

    private final int order;

    public OrderedValidatingClientInterceptor(ValidatorIndex index, int order) {
        super(index);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
