package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "grpc.test.enabled=false",
            "grpc.server.port=12345",
            "grpc.client.authority=localhost:12345",
        })
class TLSAppTest {

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub;

    @Test
    void testUnaryRpc() {
        String responseMessage = simpleServiceBlockingStub
                .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("Hello").build())
                .getResponseMessage();
        assertThat(responseMessage).isEqualTo("Hi, I got your message: Hello");
    }
}
