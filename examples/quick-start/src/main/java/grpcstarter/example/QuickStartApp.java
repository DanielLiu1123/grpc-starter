package grpcstarter.example;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuickStartApp extends SimpleServiceGrpc.SimpleServiceImplBase {

    public static void main(String[] args) {
        new SpringApplicationBuilder(QuickStartApp.class)
                .properties("grpc.client.base-packages=io.grpc")
                .properties("grpc.client.authority=127.0.0.1:9090")
                .run(args);
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> r) {
        var response = SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build();
        r.onNext(response);
        r.onCompleted();
    }

    @Bean
    ApplicationRunner runner(SimpleServiceGrpc.SimpleServiceBlockingStub stub) {
        return args -> {
            var response = stub.unaryRpc(
                    SimpleRequest.newBuilder().setRequestMessage("World!").build());
            System.out.println(response.getResponseMessage());
        };
    }
}
