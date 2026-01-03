package issues.issue77;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import io.grpc.StatusRuntimeException;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Freeman
 * @see <a href="https://github.com/DanielLiu1123/grpc-starter/pull/77">Use manually registered grpc client bean if exists</a>
 */
class Issue77Test {

    @Test
    void useManualRegisteredBean_whenManualRegisteredBeanExists() {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(CfgWithGrpcClientConfiguration.class)
                .properties("grpc.server.port=" + port)
                .properties("grpc.client.base-packages="
                        + SimpleServiceGrpc.SimpleServiceBlockingStub.class.getPackageName())
                .properties("grpc.client.authority=localhost:" + (port - 1)) // wrong authority
                .run()) {

            var stub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            var resp = stub.unaryRpc(
                    SimpleRequest.newBuilder().setRequestMessage("1").build());

            // Got the correct result, means the manual registered bean works
            assertThat(resp.getResponseMessage()).isEqualTo("Hello 1");
        }
    }

    @Test
    void useAutoRegisteredBean_whenNoManualRegisteredBeanAndUsingWrongBaseUrl_thenThrowException() {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(CfgWithoutGrpcClientConfiguration.class)
                .properties("grpc.server.port=" + port)
                .properties("grpc.client.base-packages="
                        + SimpleServiceGrpc.SimpleServiceBlockingStub.class.getPackageName())
                .properties("grpc.client.authority=localhost:" + (port - 1)) // wrong authority
                .run()) {

            var stub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            assertThatCode(() -> stub.unaryRpc(
                            SimpleRequest.newBuilder().setRequestMessage("1").build()))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("UNAVAILABLE: io exception");
        }
    }

    @Test
    void useAutoRegisteredBean_whenNoManualRegisteredBeanAndUsingCorrectBaseUrl_thenGotCorrectResult() {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(CfgWithoutGrpcClientConfiguration.class)
                .properties("grpc.server.port=" + port)
                .properties("grpc.client.base-packages="
                        + SimpleServiceGrpc.SimpleServiceBlockingStub.class.getPackageName())
                .properties("grpc.client.authority=localhost:" + port) // correct authority
                .run()) {

            var stub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            var resp = stub.unaryRpc(
                    SimpleRequest.newBuilder().setRequestMessage("1").build());

            // Got the correct result, means the auto registered bean works
            assertThat(resp.getResponseMessage()).isEqualTo("Hello 1");
        }
    }
}
