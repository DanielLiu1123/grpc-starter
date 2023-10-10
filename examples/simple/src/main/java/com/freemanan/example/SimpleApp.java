package com.freemanan.example;

import com.freemanan.foo.v1.api.FooServiceGrpc;
import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.Assert;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients({"com.freemanan", "io.grpc"})
public class SimpleApp implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(SimpleApp.class, args);
    }

    @Autowired
    BeanFactory beanFactory;

    @Override
    public void run(ApplicationArguments args) {
        Assert.notNull(beanFactory.getBean(HealthGrpc.HealthStub.class), "HealthStub should not be null");
        Assert.notNull(
                beanFactory.getBean(ServerReflectionGrpc.ServerReflectionStub.class),
                "ServerReflectionStub should not be null");
        Assert.notNull(
                beanFactory.getBean(FooServiceGrpc.FooServiceBlockingStub.class),
                "FooServiceBlockingStub should not be null");
        Assert.notNull(
                beanFactory.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class),
                "SimpleServiceBlockingStub should not be null");
    }
}
