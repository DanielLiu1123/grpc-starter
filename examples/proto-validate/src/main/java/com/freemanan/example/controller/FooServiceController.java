package com.freemanan.example.controller;

import com.freemanan.foo.v1.api.Foo;
import com.freemanan.foo.v1.api.FooServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Controller;

/**
 * @author Freeman
 */
@Controller
public class FooServiceController extends FooServiceGrpc.FooServiceImplBase {

    @Override
    public void insertFoo(Foo request, StreamObserver<Foo> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}
