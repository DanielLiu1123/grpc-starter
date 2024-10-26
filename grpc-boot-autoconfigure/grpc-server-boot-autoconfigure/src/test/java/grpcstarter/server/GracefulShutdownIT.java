package grpcstarter.server;

import static grpcstarter.extensions.test.StubUtil.createStub;
import static io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub;
import static io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceImplBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC server graceful shutdown tester.
 */
@ExtendWith(OutputCaptureExtension.class)
class GracefulShutdownIT {

    @Test
    void testGracefulShutdown(CapturedOutput output) throws InterruptedException {
        String inProcessName = UUID.randomUUID().toString();

        try (var ignored = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.in-process.name=" + inProcessName)
                .run()) {

            var stub = createStub(inProcessName, SimpleServiceBlockingStub.class);
            new Thread(() -> stub.unaryRpc(SimpleRequest.getDefaultInstance())).start();

            Thread.sleep(100);
        }

        Matcher matcher =
                Pattern.compile("gRPC server graceful shutdown in (\\d+) ms").matcher(output.getOut());
        if (!matcher.find()) {
            fail("Shutdown message not found");
        }
        long time = Long.parseLong(matcher.group(1));

        // 1000 - 100
        assertThat(time).isGreaterThanOrEqualTo(850);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceImplBase {
        @Override
        @SneakyThrows
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            Thread.sleep(1000);
            responseObserver.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage(request.getRequestMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
