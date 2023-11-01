package com.freemanan.example.controller;

import com.freemanan.foo.v1.api.Foo;
import com.freemanan.foo.v1.api.FooServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Controller;

/**
 * @author Freeman
 */
@Controller
public class FooController extends FooServiceGrpc.FooServiceImplBase {

    @Override
    public void create(Foo request, StreamObserver<Foo> responseObserver) {
        if (request.getName().length() <= 3) {
            throw new IllegalArgumentException("name length must be greater than 3");
        }
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}
