package com.freemanan.starter.grpc.extensions.observability.metrics;

import io.prometheus.client.CollectorRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
public class ObservabilityMetricsAutoConfiguration {

    public static void main(String[] args) {
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
    }
}
