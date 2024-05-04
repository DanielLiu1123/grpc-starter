package grpcstarter.example;

import foo.Foo;
import foo.FooServiceGrpc;
import grpcstarter.server.GrpcService;
import io.grpc.stub.StreamObserver;

/**
 * @author Freeman
 */
@GrpcService
public class FooServiceImpl extends FooServiceGrpc.FooServiceImplBase {

    @Override
    public void create(Foo request, StreamObserver<Foo> responseObserver) {
        if (request.getName().length() <= 3) {
            throw new IllegalArgumentException("name length must be greater than 3");
        }
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}
