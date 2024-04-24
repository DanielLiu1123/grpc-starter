package com.freemanan.example;

import grpcstarter.client.EnableGrpcClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients({"com.freemanan"})
public class ProtoValidateApp {

    public static void main(String[] args) {
        SpringApplication.run(ProtoValidateApp.class, args);
    }
}
