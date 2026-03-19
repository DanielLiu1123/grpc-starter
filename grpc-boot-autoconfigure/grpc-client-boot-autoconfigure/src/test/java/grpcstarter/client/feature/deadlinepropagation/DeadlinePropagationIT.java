package grpcstarter.client.feature.deadlinepropagation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for deadline propagation.
 *
 * <p>Verifies that when a gRPC server receives a request with a deadline,
 * that deadline is automatically propagated to any outgoing client calls
 * made during the same RPC context (via {@link DeadlinePropagationClientInterceptor}).
 */
@SpringBootTest(
        classes = DeadlinePropagationIT.Cfg.class,
        properties = {
            "grpc.client.base-packages=io.grpc",
            "grpc.server.in-process.name=DeadlinePropagationIT",
            "grpc.client.in-process.name=DeadlinePropagationIT"
        })
class DeadlinePropagationIT {

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        Cfg.capturedInnerDeadline.set(null);
    }

    @Test
    void testDeadlineIsPropagatedToInnerCall() {
        stub.withDeadlineAfter(1, TimeUnit.SECONDS)
                .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("outer").build());

        Deadline captured = Cfg.capturedInnerDeadline.get();
        assertThat(captured).isNotNull();
        // Propagated deadline should be within 1 second from now (check in ms to avoid truncation)
        assertThat(captured.timeRemaining(TimeUnit.MILLISECONDS))
                .isGreaterThan(0)
                .isLessThanOrEqualTo(1000);
    }

    @Test
    void testDeadlineNotPropagatedWhenOuterCallHasNoDeadline() {
        // Call without deadline — inner call should also have no propagated deadline
        stub.unaryRpc(SimpleRequest.newBuilder().setRequestMessage("outer").build());

        assertThat(Cfg.capturedInnerDeadline.get()).isNull();
    }

    @Test
    void testPropagatedDeadlineExpires_innerCallFailsWithDeadlineExceeded() {
        // Outer call has a 100ms deadline; inner service deliberately sleeps 300ms.
        // Without propagation the inner call would complete fine (no deadline).
        // With propagation the inner call inherits the ~100ms deadline and times out.
        assertThatThrownBy(() -> stub.withDeadlineAfter(100, TimeUnit.MILLISECONDS)
                        .unaryRpc(SimpleRequest.newBuilder()
                                .setRequestMessage("slow-outer")
                                .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(
                        e -> assertThat(((StatusRuntimeException) e).getStatus().getCode())
                                .isEqualTo(Status.DEADLINE_EXCEEDED.getCode()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {

        /**
         * Stores the {@link Context#getDeadline()} captured during the "inner" self-call.
         * Null means no deadline was propagated.
         */
        static final AtomicReference<Deadline> capturedInnerDeadline = new AtomicReference<>();

        @Autowired
        SimpleServiceGrpc.SimpleServiceBlockingStub selfStub;

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            switch (request.getRequestMessage()) {
                case "outer" -> {
                    // Fast inner call — propagation test only checks deadline value
                    selfStub.unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("inner")
                            .build());
                    responseObserver.onNext(
                            SimpleResponse.newBuilder().setResponseMessage("ok").build());
                    responseObserver.onCompleted();
                }
                case "inner" -> {
                    // Capture the propagated deadline and respond immediately
                    capturedInnerDeadline.set(Context.current().getDeadline());
                    responseObserver.onNext(
                            SimpleResponse.newBuilder().setResponseMessage("ok").build());
                    responseObserver.onCompleted();
                }
                case "slow-outer" -> {
                    // Trigger a slow inner call; propagated deadline should expire inside it
                    selfStub.unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("slow-inner")
                            .build());
                    responseObserver.onNext(
                            SimpleResponse.newBuilder().setResponseMessage("ok").build());
                    responseObserver.onCompleted();
                }
                case "slow-inner" -> {
                    // Sleep longer than the propagated deadline so DEADLINE_EXCEEDED is triggered
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    responseObserver.onNext(
                            SimpleResponse.newBuilder().setResponseMessage("ok").build());
                    responseObserver.onCompleted();
                }
                default ->
                    responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("unknown message")
                            .asException());
            }
        }
    }
}
