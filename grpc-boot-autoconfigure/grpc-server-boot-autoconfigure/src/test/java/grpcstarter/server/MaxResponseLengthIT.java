package grpcstarter.server;

import static io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub;
import static io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceImplBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import grpcstarter.extensions.test.InProcessName;
import grpcstarter.extensions.test.StubUtil;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@SpringBootTest(
        classes = MaxResponseLengthIT.Cfg.class,
        properties = {"grpc.server.response.max-description-length=20"})
class MaxResponseLengthIT {

    @InProcessName
    @SuppressWarnings("NullAway")
    String inProcessName;

    @Test
    void testMaxDescriptionLength_whenDescriptionIsTooLong_thenShouldTruncate() {
        SimpleServiceBlockingStub stub = StubUtil.createStub(inProcessName, SimpleServiceBlockingStub.class);

        // Test that the response description is not truncated
        SimpleRequest notTruncateRequest =
                SimpleRequest.newBuilder().setRequestMessage("short message").build();

        assertThatCode(() -> stub.unaryRpc(notTruncateRequest))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
                    assertThat(e.getStatus().getDescription()).isEqualTo("short message");
                });

        // Test that the response description is truncated
        SimpleRequest truncateRequest = SimpleRequest.newBuilder()
                .setRequestMessage("loooooooooong message")
                .build();
        assertThatCode(() -> stub.unaryRpc(truncateRequest)).isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
            assertThat(e.getStatus().getDescription()).isEqualTo("loooooooooong messag... (21 length)");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceImplBase {

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onError(
                    new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(request.getRequestMessage())));
        }
    }
}
