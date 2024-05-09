package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;

import grpcstarter.extensions.test.InProcessName;
import grpcstarter.extensions.test.StubUtil;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = QuickStartApp.class,
        properties = {
            "grpc.server.in-process.name=QuickStartAppTest",
            "grpc.client.in-process.name=QuickStartAppTest",
            "grpc.client.base-packages=io.grpc",
        })
class QuickStartAppTest {

    @InProcessName
    String name;

    @Test
    void testQuickStart() {
        var stub = StubUtil.createStub(name, SimpleServiceGrpc.SimpleServiceBlockingStub.class);
        var response = stub.unaryRpc(
                SimpleRequest.newBuilder().setRequestMessage("World!").build());

        assertThat(response.getResponseMessage()).isEqualTo("Hello World!");
    }
}
