package com.freemanan.starter.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Freeman
 */
@SpringBootTest(
        classes = InProcessTest.Cfg.class,
        properties = {"grpc.server.in-process.name=InProcessTest"})
class InProcessTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void testInProcess() {
        Server server = (Server) ReflectionTestUtils.getField(ctx.getBean(DefaultGrpcServer.class), "server");

        assertThat(server).isNotNull();
        assertThat(server.getPort()).isEqualTo(-1);

        ManagedChannel channel =
                InProcessChannelBuilder.forName("InProcessTest").usePlaintext().build();

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(channel);
        SimpleResponse resp = stub.unaryRpc(
                SimpleRequest.newBuilder().setRequestMessage("Hello").build());

        assertThat(resp.getResponseMessage()).isEqualTo("Hello");

        channel.shutdown();
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
