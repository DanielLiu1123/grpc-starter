package com.freeman.example;

import com.freemanan.starter.grpc.server.GrpcService;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author Freeman
 */
@GrpcService
public class SimpleServiceImpl extends SimpleServiceGrpc.SimpleServiceImplBase {
    @Override
    @PostMapping("/simple")
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> so) {
        SimpleResponse response = SimpleResponse.newBuilder()
                .setResponseMessage("Hello, I got your message: " + request.getRequestMessage())
                .build();
        so.onNext(response);
        so.onCompleted();
    }
}
