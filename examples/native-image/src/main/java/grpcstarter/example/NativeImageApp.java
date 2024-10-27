package grpcstarter.example;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.net.ServerSocket;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.util.Assert;

@Slf4j
@SpringBootApplication
public class NativeImageApp extends SimpleServiceGrpc.SimpleServiceImplBase {

    public static void main(String[] args) {

        var port = randomPort();

        try (var ctx = new SpringApplicationBuilder(NativeImageApp.class)
                .properties("grpc.server.port=" + port)
                .properties("grpc.client.base-packages=io.grpc")
                .properties("grpc.client.authority=localhost:" + port)
                .run(args)) {

            var stub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            var response = stub.unaryRpc(
                    SimpleRequest.newBuilder().setRequestMessage("World").build());

            log.info("Response message: {}", response.getResponseMessage());

            Assert.isTrue(response.getResponseMessage().equals("Hello World"), "Response message not matched");
        }
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> r) {
        var response = SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build();
        r.onNext(response);
        r.onCompleted();
    }

    @SneakyThrows
    private static int randomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
}
