package grpcstarter.example;

import static transcoding.mvc.SimpleServiceGrpc.SimpleServiceImplBase;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import transcoding.mvc.Simpleservice;
import transcoding.mvc.Simpleservice.SimpleRequest;

/**
 * @author Freeman
 */
@SpringBootApplication
public class TranscodingMvcApp extends SimpleServiceImplBase {

    public static void main(String[] args) {
        SpringApplication.run(TranscodingMvcApp.class, args);
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> r) {
        if (request.getRequestMessage().contains("err")) {
            var metadata = new Metadata();
            metadata.put(Metadata.Key.of("error", Metadata.ASCII_STRING_MARSHALLER), "invalid argument");
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT, metadata);
        }

        r.onNext(Simpleservice.SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build());
        r.onCompleted();
    }

    @Override
    @SneakyThrows
    public void serverStreamingRpc(SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> r) {
        for (int i = 0; i < 10; i++) {
            r.onNext(Simpleservice.SimpleResponse.newBuilder()
                    .setResponseMessage("message " + i)
                    .build());
            Thread.sleep(1000);
        }
        r.onCompleted();
    }
}
