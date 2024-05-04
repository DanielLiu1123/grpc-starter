package grpcstarter.example;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
public class RefreshApp extends SimpleServiceGrpc.SimpleServiceImplBase {

    public static void main(String[] args) {
        SpringApplication.run(RefreshApp.class, args);
    }

    @Override
    @SneakyThrows
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        Thread.sleep(600);
        SimpleResponse response = SimpleResponse.newBuilder()
                .setResponseMessage(request.getRequestMessage())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
