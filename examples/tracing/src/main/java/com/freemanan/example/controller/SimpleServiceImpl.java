package com.freemanan.example.controller;

import com.freemanan.example.api.SimpleApi;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Freeman
 */
@RestController
public class SimpleServiceImpl extends SimpleServiceGrpc.SimpleServiceImplBase implements SimpleApi {

    @Autowired
    SimpleApi simpleApi;

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub;

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        if (request.getRequestMessage().length() > 20) {
            responseObserver.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage(request.getRequestMessage())
                    .build());
            responseObserver.onCompleted();
        } else {
            SimpleResponse response = SimpleResponse.newBuilder()
                    .setResponseMessage(simpleApi.get("Hello " + request.getRequestMessage()))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public String get(String message) {
        if (message.length() > 20) {
            return message;
        }
        return simpleStub
                .unaryRpc(SimpleRequest.newBuilder()
                        .setRequestMessage("Hello " + message)
                        .build())
                .getResponseMessage();
    }
}
