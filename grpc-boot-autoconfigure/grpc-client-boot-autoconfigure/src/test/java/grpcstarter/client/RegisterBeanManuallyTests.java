package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class RegisterBeanManuallyTests {

    static int port = findAvailableTcpPort();

    @Test
    void useAutoRegisteredBean_whenNoManualRegisteredBean() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.server.port=" + port)
                .properties(
                        "grpc.client.authority=localhost:" + (port - 1)) // Auto register a bean with wrong authority
                .run()) {

            var stub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            assertThatCode(() -> stub.unaryRpc(SimpleRequest.getDefaultInstance()))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("io exception");
        }
    }

    @Test
    void useManualRegisteredBean_whenManualRegisteredBeanExists() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class, StubCfg.class)
                .properties("grpc.server.port=" + port)
                .properties(
                        "grpc.client.authority=localhost:" + (port - 1)) // Auto register a bean with wrong authority
                .run()) {

            var stub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            var response = stub.unaryRpc(
                    SimpleRequest.newBuilder().setRequestMessage("World!").build());
            assertThat(response.getResponseMessage()).isEqualTo("Hello World!");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableGrpcClients(clients = SimpleServiceGrpc.SimpleServiceBlockingStub.class)
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            var response = SimpleResponse.newBuilder()
                    .setResponseMessage("Hello " + request.getRequestMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StubCfg {
        @Bean
        public SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub() {
            var channel = ManagedChannelBuilder.forAddress(
                            "localhost", port) // Manually register a bean with correct authority
                    .usePlaintext()
                    .build();
            return SimpleServiceGrpc.newBlockingStub(channel);
        }
    }
}
