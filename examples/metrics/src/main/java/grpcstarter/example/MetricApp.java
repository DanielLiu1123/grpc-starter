package grpcstarter.example;

import grpcstarter.client.EnableGrpcClients;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients("io.grpc")
public class MetricApp {

    public static void main(String[] args) {
        SpringApplication.run(MetricApp.class, args);
    }

    @Bean
    ApplicationRunner runner(SimpleServiceGrpc.SimpleServiceBlockingStub stub) {
        return args -> {
            SimpleResponse resp = stub.unaryRpc(
                    SimpleRequest.newBuilder().setRequestMessage("Hello").build());
            System.out.println(resp);
        };
    }
}
