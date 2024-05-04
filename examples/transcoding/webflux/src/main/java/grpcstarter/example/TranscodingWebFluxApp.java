package grpcstarter.example;

import static grpcstarter.server.GrpcContextKeys.ResponseMetadataModifier;
import static transcoding.flux.SimpleServiceGrpc.SimpleServiceImplBase;
import static transcoding.flux.Simpleservice.SimpleRequest;
import static transcoding.flux.Simpleservice.SimpleResponse;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Freeman
 */
@SpringBootApplication
public class TranscodingWebFluxApp extends SimpleServiceImplBase {

    public static void main(String[] args) {
        SpringApplication.run(TranscodingWebFluxApp.class, args);
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> r) {
        if (request.getRequestMessage().contains("err")) {
            var metadata = new Metadata();
            metadata.put(Metadata.Key.of("error", Metadata.ASCII_STRING_MARSHALLER), "invalid argument");
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT, metadata);
        }

        ResponseMetadataModifier.addConsumer(
                metadata -> metadata.put(Metadata.Key.of("custom", Metadata.ASCII_STRING_MARSHALLER), "custom value"));
        r.onNext(SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build());
        r.onCompleted();
    }

    @Override
    @SneakyThrows
    public void serverStreamingRpc(SimpleRequest request, StreamObserver<SimpleResponse> r) {
        for (int i = 0; i < 10; i++) {
            r.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("message " + i)
                    .build());
            Thread.sleep(1000);
        }
        r.onCompleted();
    }
}
