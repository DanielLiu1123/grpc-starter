package com.freemanan.example;

import static com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub;
import static io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.foo.v1.api.Foo;
import com.freemanan.starter.grpc.extensions.test.InProcessName;
import com.freemanan.starter.grpc.extensions.test.StubUtil;
import io.grpc.StatusRuntimeException;
import io.grpc.testing.protobuf.SimpleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SimpleAppTest {

    @InProcessName
    String name;

    @Test
    void testCreateFoo() {
        FooServiceBlockingStub stub = StubUtil.createStub(name, FooServiceBlockingStub.class);
        Foo foo = stub.create(Foo.newBuilder().setId("001").setName("Freeman").build());
        assertThat(foo.getId()).isEqualTo("001");
        assertThat(foo.getName()).isEqualTo("Freeman");
    }

    @Test
    void testCreateFoo_whenServerErrorAndNoExceptionHandler_thenClientSideShouldGetUnknownCode() {
        FooServiceBlockingStub stub = StubUtil.createStub(name, FooServiceBlockingStub.class);
        Foo foo = Foo.newBuilder().setId("002").setName("Fre").build();
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> stub.create(foo))
                .withMessageContaining("UNKNOWN");
    }

    @Test
    void testUnaryRpc() {
        SimpleServiceBlockingStub stub = StubUtil.createStub(name, SimpleServiceBlockingStub.class);
        String responseMessage = stub.unaryRpc(
                        SimpleRequest.newBuilder().setRequestMessage("Hello").build())
                .getResponseMessage();
        assertThat(responseMessage).isEqualTo("Hi, I got your message: Hello");
    }
}
