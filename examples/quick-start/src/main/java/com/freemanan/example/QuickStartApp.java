package com.freemanan.example;

import com.freemanan.foo.v1.api.FooServiceGrpc;
import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients({"com.freemanan", "io.grpc"})
public class QuickStartApp {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApp.class, args);
    }

    @Bean
    ApplicationRunner runner(
            BeanFactory beanFactory,
            HealthGrpc.HealthStub healthStub,
            ServerReflectionGrpc.ServerReflectionStub serverReflectionStub,
            FooServiceGrpc.FooServiceBlockingStub fooServiceBlockingStub,
            SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub) {
        return args -> {};
    }
}
