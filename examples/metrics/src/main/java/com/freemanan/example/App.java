package com.freemanan.example;

import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients("io.grpc")
public class App implements ApplicationRunner {
    private final Counter counter;

    public App(MeterRegistry mr) {
        this.counter = Counter.builder("app_run")
                .description("app run")
                .tags("app", "metrics-test")
                .register(mr);
    }

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub stub;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        counter.increment();
        SimpleResponse resp = stub.unaryRpc(
                SimpleRequest.newBuilder().setRequestMessage("Hello").build());
        System.out.println(resp);
    }
}
