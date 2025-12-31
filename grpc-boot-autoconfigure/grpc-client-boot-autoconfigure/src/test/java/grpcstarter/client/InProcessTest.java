package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;

import grpcstarter.server.GrpcServer;
import grpcstarter.server.GrpcService;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@SpringBootTest(
        classes = InProcessTest.Cfg.class,
        properties = {
            "grpc.client.base-packages=io.grpc",
            "grpc.server.in-process.name=InProcessChannelTest",
            "grpc.client.in-process.name=InProcessChannelTest"
        })
class InProcessTest {

    @Autowired
    ApplicationContext ctx;

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub stub;

    @Test
    void testInProcess() {
        var server = ctx.getBean(GrpcServer.class);

        assertThat(server.getPort()).isEqualTo(-1);

        SimpleResponse resp = stub.unaryRpc(
                SimpleRequest.newBuilder().setRequestMessage("Hello").build());

        assertThat(resp.getResponseMessage()).isEqualTo("Hello");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @GrpcService
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage(request.getRequestMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
