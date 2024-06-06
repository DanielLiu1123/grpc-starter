package grpcstarter.examples;

import foo.FooOuterClass;
import foo.FooServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
public class ProtoValidateApp extends FooServiceGrpc.FooServiceImplBase {

    public static void main(String[] args) {
        SpringApplication.run(ProtoValidateApp.class, args);
    }

    @Override
    public void insertFoo(FooOuterClass.Foo request, StreamObserver<FooOuterClass.Foo> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}
