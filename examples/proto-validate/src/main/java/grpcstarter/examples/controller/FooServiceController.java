package grpcstarter.examples.controller;

import static grpcstarter.examples.protovalidate.FooOuterClass.Foo;

import grpcstarter.examples.protovalidate.FooServiceGrpc;
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
