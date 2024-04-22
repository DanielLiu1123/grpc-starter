package com.freeman.example;

import static com.freemanan.starter.grpc.server.GrpcContextKeys.ResponseMetadataModifier;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import transcoding.flux.SimpleServiceGrpc;
import transcoding.flux.Simpleservice;

/**
 * @author Freeman
 */
@SpringBootApplication
public class WebFluxApp extends SimpleServiceGrpc.SimpleServiceImplBase {
    public static void main(String[] args) {
        SpringApplication.run(WebFluxApp.class, args);
    }

    @Override
    public void unaryRpc(
            Simpleservice.SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> responseObserver) {
        if (request.getRequestMessage().contains("err")) {
            var metadata = new Metadata();
            metadata.put(Metadata.Key.of("error", Metadata.ASCII_STRING_MARSHALLER), "invalid argument");
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT, metadata);
        }

        ResponseMetadataModifier.addConsumers(metadata -> {
            metadata.put(Metadata.Key.of("custom", Metadata.ASCII_STRING_MARSHALLER), "custom value");
        });
        responseObserver.onNext(Simpleservice.SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @SneakyThrows
    public void serverStreamingRpc(
            Simpleservice.SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> responseObserver) {
        for (int i = 0; i < 100; i++) {
            responseObserver.onNext(Simpleservice.SimpleResponse.newBuilder()
                    .setResponseMessage("message " + i)
                    .build());
            Thread.sleep(1000);
        }
        responseObserver.onCompleted();
    }
}
