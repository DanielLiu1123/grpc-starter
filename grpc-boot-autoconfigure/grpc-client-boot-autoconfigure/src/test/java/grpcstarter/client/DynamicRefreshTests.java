package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
class DynamicRefreshTests {

    static int port1 = findAvailableTcpPort();
    static int port2 = findAvailableTcpPort();
    static Server server1;
    static Server server2;

    static {
        server1 = getServer(port1, "v1");
        server2 = getServer(port2, "v2");
    }

    @BeforeAll
    static void startServers() throws Exception {
        server1.start();
        server2.start();
    }

    @AfterAll
    static void stopServers() {
        server1.shutdownNow();
        server2.shutdownNow();
    }

    @Test
    void testDynamicRefresh() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.refresh.enabled=true")
                .properties("grpc.client.authority=localhost:" + port1)
                .properties("grpc.server.enabled=false")
                .run();

        SimpleServiceGrpc.SimpleServiceBlockingStub stub =
                ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

        SimpleResponse response = stub.unaryRpc(SimpleRequest.getDefaultInstance());
        assertThat(response.getResponseMessage()).isEqualTo("v1");

        System.setProperty("grpc.client.authority", "localhost:" + port2);
        System.setProperty("grpc.client.deadline", "2000");
        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        response = stub.unaryRpc(SimpleRequest.getDefaultInstance());
        assertThat(response.getResponseMessage()).isEqualTo("v2");

        System.clearProperty("grpc.client.authority");
        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableGrpcClients(clients = SimpleServiceGrpc.SimpleServiceBlockingStub.class)
    static class Cfg {}

    private static Server getServer(int port, String returnValue) {
        return ServerBuilder.forPort(port)
                .addService(new SimpleServiceGrpc.SimpleServiceImplBase() {
                    @Override
                    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                        responseObserver.onNext(SimpleResponse.newBuilder()
                                .setResponseMessage(returnValue)
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build();
    }
}
