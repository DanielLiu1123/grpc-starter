package grpcstarter.server;

import static grpcstarter.server.GrpcContextKeys.ResponseMetadataModifier;
import static io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub;
import static io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceImplBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import grpcstarter.extensions.test.InProcessName;
import grpcstarter.extensions.test.StubUtil;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@SpringBootTest(classes = GrpcResponseMetadataModifierIT.Cfg.class)
class GrpcResponseMetadataModifierIT {

    @InProcessName
    String inProcessName;

    @Test
    void testGrpcResponseMetadataModifier_whenSetNewHeader_thenClientShouldReceive() {
        SimpleServiceBlockingStub simpleStub = StubUtil.createStub(inProcessName, SimpleServiceBlockingStub.class);

        AtomicReference<Metadata> responseHeaders = new AtomicReference<>();
        AtomicReference<Metadata> responseTrailers = new AtomicReference<>();
        SimpleServiceBlockingStub stub = simpleStub.withInterceptors(
                MetadataUtils.newCaptureMetadataInterceptor(responseHeaders, responseTrailers));

        // Test set metadata to normal response headers
        SimpleRequest okRequest =
                SimpleRequest.newBuilder().setRequestMessage("ok").build();
        SimpleResponse okResponse = stub.unaryRpc(okRequest);

        assertThat(okResponse.getResponseMessage()).isEqualTo("ok");
        assertThat(responseHeaders.get().get(Metadata.Key.of("status", Metadata.ASCII_STRING_MARSHALLER)))
                .isEqualTo("ok");

        // Test set metadata to error response trailers
        SimpleRequest errorRequest =
                SimpleRequest.newBuilder().setRequestMessage("error").build();

        assertThatCode(() -> stub.unaryRpc(errorRequest)).isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(Status.INVALID_ARGUMENT);
            assertThat(responseTrailers.get().get(Metadata.Key.of("status", Metadata.ASCII_STRING_MARSHALLER)))
                    .isEqualTo("error");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceImplBase {

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            if (request.getRequestMessage().equalsIgnoreCase("error")) {
                Metadata trailers = new Metadata();
                trailers.put(Metadata.Key.of("status", Metadata.ASCII_STRING_MARSHALLER), "error");
                responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT, trailers));
                return;
            }

            SimpleResponse response = SimpleResponse.newBuilder()
                    .setResponseMessage(request.getRequestMessage())
                    .build();

            ResponseMetadataModifier.addConsumer(
                    metadata -> metadata.put(Metadata.Key.of("status", Metadata.ASCII_STRING_MARSHALLER), "ok"));

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
