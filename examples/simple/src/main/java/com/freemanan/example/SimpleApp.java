package com.freemanan.example;

import com.freemanan.foo.v1.api.FooServiceGrpc;
import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients({"com.freemanan", "io.grpc"})
public class SimpleApp implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SimpleApp.class);

    public static void main(String[] args) {
        SpringApplication.run(SimpleApp.class, args);
    }

    @Autowired
    HealthGrpc.HealthBlockingStub healthBlockingStub;

    @Autowired
    HealthGrpc.HealthStub healthStub;

    @Autowired
    ServerReflectionGrpc.ServerReflectionStub reflectionStub;

    @Autowired
    FooServiceGrpc.FooServiceBlockingStub fooStub;

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub;

    @Override
    public void run(ApplicationArguments args) {
        log.info(healthBlockingStub
                .check(HealthCheckRequest.newBuilder().build())
                .toString());
        StreamObserver<ServerReflectionRequest> so =
                reflectionStub.serverReflectionInfo(new StreamObserver<ServerReflectionResponse>() {
                    @Override
                    public void onNext(ServerReflectionResponse value) {
                        log.info(value.toString());
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onCompleted() {
                        log.info("onCompleted");
                    }
                });
        so.onNext(ServerReflectionRequest.newBuilder().setListServices("").build());
    }
}
