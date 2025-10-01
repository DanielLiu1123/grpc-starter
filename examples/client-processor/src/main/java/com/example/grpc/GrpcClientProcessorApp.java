package com.example.grpc;

import grpcstarter.client.GenerateGrpcClients;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Application entry point.
 *
 * @author Freeman
 */
@SpringBootApplication
@GenerateGrpcClients(basePackages = {"com.example.grpc", "io.grpc.testing.protobuf", "io.grpc.health.v1"})
public class GrpcClientProcessorApp {

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientProcessorApp.class, args);
    }

    @Bean
    ApplicationRunner runner(
            HelloServiceGrpc.HelloServiceBlockingStub stub,
            SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub,
            HealthGrpc.HealthBlockingV2Stub healthStub) {
        return args -> {};
    }
}
