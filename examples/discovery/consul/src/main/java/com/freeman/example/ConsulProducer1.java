package com.freeman.example;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Freeman
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class ConsulProducer1 extends SimpleServiceGrpc.SimpleServiceImplBase {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ConsulProducer1.class)
                .profiles("producer1")
                .run(args);
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        responseObserver.onNext(SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage() + " from producer1")
                .build());
        responseObserver.onCompleted();
    }
}
