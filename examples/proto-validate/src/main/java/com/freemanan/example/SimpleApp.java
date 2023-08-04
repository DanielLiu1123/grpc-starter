package com.freemanan.example;

import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients({"io.grpc"})
public class SimpleApp implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(SimpleApp.class, args);
    }

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub;

    @Override
    public void run(ApplicationArguments args) {
        System.out.println(simpleStub.unaryRpc(SimpleRequest.newBuilder().build()));
    }
}
