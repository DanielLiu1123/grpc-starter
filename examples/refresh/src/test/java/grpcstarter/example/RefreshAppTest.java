package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.grpc.StatusRuntimeException;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
        properties = {
            "grpc.test.enabled=false",
            "grpc.server.port=52330",
            "grpc.client.authority=localhost:${grpc.server.port}",
        })
class RefreshAppTest {

    @Autowired
    SimpleServiceBlockingStub simpleStub;

    @Autowired
    ApplicationContext ctx;

    static SimpleRequest request =
            SimpleRequest.newBuilder().setRequestMessage("Hello").build();

    @Test
    void testDeadline() {
        normalRequest();

        // change deadline to 500ms
        System.setProperty("grpc.client.deadline", "500");

        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        assertThatCode(() -> simpleStub.unaryRpc(request))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("DEADLINE_EXCEEDED: CallOptions deadline exceeded after");

        System.clearProperty("grpc.client.deadline");
    }

    @Test
    @Disabled("Can't gracefully exit in this test case")
    void testMaxOutboundMessageSize() {
        normalRequest();

        // change max outbound message size to 1B
        System.setProperty("grpc.client.max-outbound-message-size", "3B");

        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        assertThatCode(() -> simpleStub.unaryRpc(request))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("INTERNAL: Failed to frame message");

        System.clearProperty("grpc.client.max-outbound-message-size");
    }

    @Test
    void testCompression() {
        normalRequest();

        // change compression to identity
        System.setProperty("grpc.client.compression", "identity");

        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        assertThatCode(() -> simpleStub.unaryRpc(request)).doesNotThrowAnyException();

        System.clearProperty("grpc.client.compression");
    }

    private void normalRequest() {
        String responseMessage = simpleStub.unaryRpc(request).getResponseMessage();

        assertThat(responseMessage).isEqualTo("Hello");
    }
}
