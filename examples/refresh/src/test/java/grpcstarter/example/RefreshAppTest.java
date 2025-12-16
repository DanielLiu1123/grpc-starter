package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import io.grpc.StatusRuntimeException;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.endpoint.event.RefreshEvent;

class RefreshAppTest {

    @Test
    void testDeadline() {
        var port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(RefreshApp.class)
                .properties("grpc.server.port= " + port)
                .properties("grpc.client.authority=localhost:" + port)
                .run()) {

            var simpleStub = ctx.getBean(SimpleServiceBlockingStub.class);
            var responseMessage = simpleStub
                    .unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("Hello")
                            .build())
                    .getResponseMessage();

            assertThat(responseMessage).isEqualTo("Hello");

            // change deadline to 500ms
            System.setProperty("grpc.client.deadline", "500");
            ctx.publishEvent(new RefreshEvent(ctx, null, null));

            assertThatCode(() -> simpleStub.unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("Hello")
                            .build()))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("DEADLINE_EXCEEDED: CallOptions deadline exceeded after");
        } finally {
            System.clearProperty("grpc.client.deadline");
        }
    }

    @Test
    @Disabled("Can't gracefully exit in this test case")
    void testMaxOutboundMessageSize() {
        var port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(RefreshApp.class)
                .properties("grpc.server.port= " + port)
                .properties("grpc.client.authority=localhost:" + port)
                .run()) {

            var simpleStub = ctx.getBean(SimpleServiceBlockingStub.class);
            var responseMessage = simpleStub
                    .unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("Hello")
                            .build())
                    .getResponseMessage();

            assertThat(responseMessage).isEqualTo("Hello");

            // change max outbound message size to 1B
            System.setProperty("grpc.client.max-outbound-message-size", "1B");
            ctx.publishEvent(new RefreshEvent(ctx, null, null));

            assertThatCode(() -> simpleStub.unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("Hello")
                            .build()))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("RESOURCE_EXHAUSTED: message too large");
        } finally {
            System.clearProperty("grpc.client.max-outbound-message-size");
        }
    }

    @Test
    void testCompression() {
        var port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(RefreshApp.class)
                .properties("grpc.server.port= " + port)
                .properties("grpc.client.authority=localhost:" + port)
                .run()) {

            var simpleStub = ctx.getBean(SimpleServiceBlockingStub.class);
            var responseMessage = simpleStub
                    .unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("Hello")
                            .build())
                    .getResponseMessage();

            assertThat(responseMessage).isEqualTo("Hello");

            // change compression to gzip
            System.setProperty("grpc.client.compression", "gzip");
            ctx.publishEvent(new RefreshEvent(ctx, null, null));

            assertThatCode(() -> simpleStub.unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("Hello")
                            .build()))
                    .doesNotThrowAnyException();
        } finally {
            System.clearProperty("grpc.client.max-outbound-message-size");
        }
    }
}
