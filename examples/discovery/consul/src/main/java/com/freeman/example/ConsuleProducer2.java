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
public class ConsuleProducer2 extends SimpleServiceGrpc.SimpleServiceImplBase {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ConsuleProducer2.class)
                .profiles("producer2")
                .run(args);
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        responseObserver.onNext(SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage() + " from producer2")
                .build());
        responseObserver.onCompleted();
    }
}
