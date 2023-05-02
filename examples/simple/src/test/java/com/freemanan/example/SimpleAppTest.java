package com.freemanan.example;

import static com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.foo.v1.api.Foo;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "grpc.server.in-process.name=test",
            "grpc.client.in-process.name=test",
        })
class SimpleAppTest {

    @Autowired
    private FooServiceBlockingStub fooBlockingStub;

    @Test
    void testInProcessTransport() {
        Foo foo = fooBlockingStub.create(
                Foo.newBuilder().setId("001").setName("Freeman").build());
        assertThat(foo.getId()).isEqualTo("001");
        assertThat(foo.getName()).isEqualTo("Freeman");
    }

    @Test
    void testBehavior_whenNoExceptionHandler() {
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> fooBlockingStub.create(
                        Foo.newBuilder().setId("002").setName("Fre").build()))
                .withMessageContaining("UNKNOWN");
    }
}
