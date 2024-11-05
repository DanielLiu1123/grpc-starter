package grpcstarter.examples;

import foo.FooOuterClass;
import foo.FooServiceGrpc;
import grpcstarter.server.GrpcServer;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

/**
 * @author Freeman
 */
@SpringBootApplication
public class ValidationApp extends FooServiceGrpc.FooServiceImplBase {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(ValidationApp.class, args);

        if (System.getenv("CI") != null) {
            ctx.close();
        }
    }

    @Override
    public void insertFoo(FooOuterClass.Foo request, StreamObserver<FooOuterClass.Foo> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

    @Bean
    ApplicationRunner runner(GrpcServer server, @Value("${CI:false}") boolean ci) {
        return args -> {
            if (!ci) {
                return;
            }
            var channel = ManagedChannelBuilder.forAddress("localhost", server.getPort())
                    .usePlaintext()
                    .build();
            var stub = FooServiceGrpc.newBlockingStub(channel);
            var request = FooOuterClass.Foo.newBuilder()
                    .setName("x") // invalid
                    .addHobbies("bar")
                    .build();

            try {
                stub.insertFoo(request);
            } catch (StatusRuntimeException e) {
                Assert.isTrue(
                        e.getMessage()
                                .contains(
                                        "INVALID_ARGUMENT: .foo.Foo.name: length must be at least 2 but got: 1 - Got \"x\""),
                        "Message not match");
            }
        };
    }
}
