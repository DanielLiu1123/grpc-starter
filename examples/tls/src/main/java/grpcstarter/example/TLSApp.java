package grpcstarter.example;

import grpcstarter.client.EnableGrpcClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients({"io.grpc"})
public class TLSApp {

    public static void main(String[] args) {
        SpringApplication.run(TLSApp.class, args);
    }
}
