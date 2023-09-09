package com.freeman.example;

import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * @author Freeman
 */
@Slf4j
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableGrpcClients("io.grpc")
@LoadBalancerClients(defaultConfiguration = CustomLoadBalancerConfig.class)
public class ConsulConsumer {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ConsulConsumer.class).profiles("consumer").run(args);
    }

    @Bean
    ApplicationRunner runner(SimpleServiceGrpc.SimpleServiceBlockingStub stub) {
        return args -> {
            for (int i = 0; i < 1000; i++) {
                log.info(stub.unaryRpc(SimpleRequest.newBuilder().build()).getResponseMessage());
            }
        };
    }
}

class CustomLoadBalancerConfig {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
            Environment environment, LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), name);
    }
}
