package grpcstarter.extensions.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * {@link InProcessName} tester.
 *
 * @author Freeman
 */
@SpringBootTest(classes = InProcessNameIT.Cfg.class)
class InProcessNameIT {

    @InProcessName
    String name;

    @Test
    void testInProcessName() {
        var stub = StubUtil.createStub(name, SimpleServiceGrpc.SimpleServiceBlockingStub.class);
        var responseMessage = stub.unaryRpc(
                        SimpleRequest.newBuilder().setRequestMessage("World!").build())
                .getResponseMessage();

        assertThat(responseMessage).isEqualTo("Hello World!");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hello " + request.getRequestMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
