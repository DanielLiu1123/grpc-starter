package issues.issue77;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class CfgWithoutHttpClientConfiguration extends SimpleServiceGrpc.SimpleServiceImplBase {

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        var resp = SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }
}
