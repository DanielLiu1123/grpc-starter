package com.freemanan.example;

import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.github.danielliu1123.httpexchange.EnableExchangeClients;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
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
@EnableGrpcClients("io.grpc")
@EnableExchangeClients
public class App implements ApplicationRunner {

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub stub;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        SimpleResponse resp = stub.unaryRpc(
                SimpleRequest.newBuilder().setRequestMessage("Hello").build());
        System.out.println(resp);
    }
}
