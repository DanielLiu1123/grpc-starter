package grpcstarter.example;

import grpcstarter.client.EnableGrpcClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients({"foo", "io.grpc"})
public class QuickStartApp {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApp.class, args);
    }
}
