package com.freemanan.starter.grpc.client;

import com.freemanan.starter.grpc.extensions.test.NetUtil;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.assertj.core.api.Assertions;
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

    static int port1 = NetUtil.getRandomPort();
    static Server server1;
    static int port2 = NetUtil.getRandomPort();
    static Server server2;

    static {
        server1 = ServerBuilder.forPort(port1)
                .addService(new SimpleServiceGrpc.SimpleServiceImplBase() {
                    @Override
                    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                        responseObserver.onNext(SimpleResponse.newBuilder()
                                .setResponseMessage("v1")
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build();
        server2 = ServerBuilder.forPort(port2)
                .addService(new SimpleServiceGrpc.SimpleServiceImplBase() {
                    @Override
                    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                        responseObserver.onNext(SimpleResponse.newBuilder()
                                .setResponseMessage("v2")
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build();
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
        Assertions.assertThat(response.getResponseMessage()).isEqualTo("v1");

        System.setProperty("grpc.client.authority", "localhost:" + port2);
        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        response = stub.unaryRpc(SimpleRequest.getDefaultInstance());
        Assertions.assertThat(response.getResponseMessage()).isEqualTo("v2");

        System.clearProperty("grpc.client.authority");
        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableGrpcClients(clients = SimpleServiceGrpc.SimpleServiceBlockingStub.class)
    static class Cfg {}
}