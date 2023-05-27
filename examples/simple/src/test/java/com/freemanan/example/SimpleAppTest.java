package com.freemanan.example;

import static com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.foo.v1.api.Foo;
import io.grpc.StatusRuntimeException;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SimpleAppTest {

    @Autowired
    FooServiceBlockingStub fooBlockingStub;

    @Resource
    SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub;

    @Test
    void testCreateFoo() {
        Foo foo = fooBlockingStub.create(
                Foo.newBuilder().setId("001").setName("Freeman").build());
        assertThat(foo.getId()).isEqualTo("001");
        assertThat(foo.getName()).isEqualTo("Freeman");
    }

    @Test
    void testCreateFoo_whenServerErrorAndNoExceptionHandler_thenClientSideShouldGetUnknownCode() {
        Foo foo = Foo.newBuilder().setId("002").setName("Fre").build();
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> fooBlockingStub.create(foo))
                .withMessageContaining("UNKNOWN");
    }

    @Test
    void testUnaryRpc() {
        String responseMessage = simpleServiceBlockingStub
                .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("Hello").build())
                .getResponseMessage();
        assertThat(responseMessage).isEqualTo("Hi, I got your message: Hello");
    }
}
