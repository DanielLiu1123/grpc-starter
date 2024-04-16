package com.freeman.example;

import com.freemanan.starter.grpc.server.GrpcService;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import transcoding.mvc.SimpleServiceGrpc;
import transcoding.mvc.Simpleservice;

/**
 * @author Freeman
 */
@GrpcService
public class SimpleServiceImpl extends SimpleServiceGrpc.SimpleServiceImplBase {

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
