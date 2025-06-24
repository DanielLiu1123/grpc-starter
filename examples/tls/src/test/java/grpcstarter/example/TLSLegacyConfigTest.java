package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test for legacy TLS configuration to ensure backward compatibility.
 * This test should show deprecation warnings in the logs.
 */
@SpringBootTest(
        properties = {
            "grpc.test.enabled=false",
            "grpc.server.port=12346",
            "grpc.client.authority=localhost:12346",
        })
@ActiveProfiles("legacy")
class TLSLegacyConfigTest {

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub;

    @Test
    void testUnaryRpcWithLegacyConfig() {
        String responseMessage = simpleServiceBlockingStub
                .unaryRpc(SimpleRequest.newBuilder()
                        .setRequestMessage("Hello Legacy")
                        .build())
                .getResponseMessage();
        assertThat(responseMessage).isEqualTo("Hi, I got your message: Hello Legacy");
    }
}
