package com.freemanan.example;

import static com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub;
import static com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceStub;

import com.freemanan.foo.v1.api.Foo;
import com.freemanan.starter.grpc.client.EnableGrpcClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients(clients = {FooServiceBlockingStub.class, FooServiceStub.class})
public class SimpleApp implements ApplicationRunner {
    public static void main(String[] args) {
        SpringApplication.run(SimpleApp.class, args);
    }

    @Autowired
    private FooServiceBlockingStub fooBlockingStub;

    @Autowired
    private FooServiceStub fooStub;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        fooBlockingStub.create(Foo.newBuilder().setId("111111").setName("naae").build());
    }
}
