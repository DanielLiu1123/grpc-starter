package com.freeman.example;

import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

/**
 * @author Freeman
 */
@Slf4j
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableGrpcClients("io.grpc")
public class EurekaConsumer {
    public static void main(String[] args) {
        new SpringApplicationBuilder(EurekaConsumer.class).profiles("consumer").run(args);
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
